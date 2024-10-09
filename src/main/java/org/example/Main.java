package org.example;

import soot.*;
import soot.jimple.infoflow.collect.ConcurrentHashSet;
import soot.options.Options;
import soot.jimple.*;
import soot.util.Chain;

import java.util.*;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.*;
import java.util.Collections;
import java.util.logging.Formatter;

// Press Shift twice to open the Search Everywhere dialog and type `show whitespaces`,
// then press Enter. You can now see whitespace characters in your code.
public class Main {
    static class CustomFormatter extends Formatter {
        @Override
        public String format(LogRecord record) {
            // This format will show: level: [sourceClass.sourceMethod] message
            return String.format("%s: %s \n",
                    record.getLevel(),
                    record.getMessage());
        }
    }
    public static Logger log = Logger.getLogger(Main.class.getName());

    public static void setupLogging() {
        // Add better formatting later.
        log.setLevel(Level.INFO);
        ConsoleHandler handler = new ConsoleHandler();
        handler.setFormatter(new CustomFormatter());
        log.addHandler(handler);
    }

    public static void setupSoot(String[] args) {
        // Setup Soot.
        G.reset();
        Options.v().set_allow_phantom_refs(true);
        Options.v().set_whole_program(true);
        Options.v().set_prepend_classpath(true);
        Options.v().set_validate(true);
        Options.v().set_src_prec(Options.src_prec_apk);
//        Options.v().set_output_format(Options.output_format_dex);
        Options.v().set_output_format(Options.output_format_none);
        Options.v().set_android_jars(args[0]);
        Options.v().set_process_dir(Collections.singletonList(args[1]));
        Options.v().set_include_all(true);
        Options.v().set_process_multiple_dex(true);
        Options.v().set_output_dir("./tmp");
        Options.v().set_force_overwrite(true);

    }

    public static SootMethod getCallee(InvokeExpr ie) {
        SootMethod met = null;

        if (ie instanceof VirtualInvokeExpr || ie instanceof InterfaceInvokeExpr) {
            // For virtual or interface invocations, use getMethodRef()
            SootMethodRef calleeMethodRef = ie.getMethodRef();
            SootMethod calleeMethod = calleeMethodRef.resolve();
            return calleeMethod;
        } else if (ie instanceof StaticInvokeExpr || ie instanceof SpecialInvokeExpr) {
            // For static or special invocations, use getMethod()
            SootMethod calleeMethod = ie.getMethod();
            return calleeMethod;
        } else {
            return met;
        }
    }

    public static void main(String[] args) {

        if(args.length < 2) {
            System.out.println("Usage: java -jar iha.jar <path to android jars> <path to apk>");
            System.exit(1);
        }

        setupSoot(args);
        setupLogging();

        Scene.v().loadNecessaryClasses();
        Scene.v().loadBasicClasses();
        Scene.v().loadDynamicClasses();

        ConcurrentHashMap<String, String> ihaFiltered = new ConcurrentHashMap<>();
        ConcurrentHashSet<String> injectedSinks = new ConcurrentHashSet<>();
        ConcurrentHashSet<String> nativeAnalysisRequired = new ConcurrentHashSet<>();

        PackManager.v().getPack("jap").add(new Transform("jap.iha", new BodyTransformer() {
            @Override
            protected void internalTransform(Body body, String phaseName, Map<String, String> map) {
                SootMethod smet = body.getMethod();
                if (Utils.isAndroidMethod(smet)) {
                    return;
                }

                if (Utils.isInjectedSink(smet)) {
                    injectedSinks.add(smet.getBytecodeSignature());
                }

                String met = smet.getName();
                String clx = smet.getDeclaringClass().getName();

                Chain<Unit> units = body.getUnits();

                Map<SootMethod, InvokeExpr> filters = new HashMap<SootMethod, InvokeExpr>();

                for (Unit unit : units) {

                    if (unit instanceof InvokeStmt is) {
                        InvokeExpr ie = is.getInvokeExpr();
                        analyzeCallee(smet, met, clx, ie, ihaFiltered);
                    }

                    if (unit instanceof AssignStmt as) {
                        if (as.getRightOp() instanceof InvokeExpr ie) {
                            analyzeCallee(smet, met, clx, ie, ihaFiltered);
                        }
                    }
                }
            }
        }));

        // For methods with JNI, we need to maintain a db of how these methods are used.
        // For now, we will just maintain a record of how many times the arguments to the
        // native method are read and written to.
//        PackManager.v().getPack("jap").add(new Transform("jap.native", new BodyTransformer() {
//            @Override
//            protected void internalTransform(Body body, String phaseName, Map<String, String> map) {
//                Chain<Unit> units = body.getUnits();
//                SootMethod smet = body.getMethod();
//
//                if (Utils.isAndroidMethod(smet)) {
//                    return;
//                }
//
//
//                for(Unit unit: units) {
//                    InvokeExpr iex = null;
//                    ArgumentAnalysisResult ret = null;
//                    if(unit instanceof InvokeStmt) {
//                        iex = ((InvokeStmt) unit).getInvokeExpr();
//                    }
//                    else if (unit instanceof AssignStmt) {
//                        Value right = ((AssignStmt) unit).getRightOp();
//                        if(right instanceof InvokeExpr) {
//                            iex = (InvokeExpr) right;
//                        }
//                    }
//
//                    if (iex != null && (iex.getMethod().isNative())){
//                        nativeAnalysisRequired.add(smet.getBytecodeSignature());
//                    }
//                    if(ret != null) {
////                        log.info("[native] " + smet.getSignature() + "result: " + ret.toString());
////                        log.info("[native] " + smet.getSignature());
//                    }
//                }
//            }
//        }));

        PackManager.v().runPacks();

        // We don't need to write the output, but we do need this function to wait till the analysis
        // Finishes so that we can print final results.
        PackManager.v().writeOutput();

        for (SootClass sootClass : Scene.v().getApplicationClasses()) {
            for (SootMethod method : sootClass.getMethods()) {
                if (method.isNative()) {
                    nativeAnalysisRequired.add(method.getBytecodeSignature());
                }
            }
        }

        System.out.println("IHA filtered methods: " + ihaFiltered);
        System.out.println("Injected sinks: " + injectedSinks);
        System.out.println("Native analysis required: " + nativeAnalysisRequired);
    }

    private static void analyzeCallee(SootMethod smet, String met, String clx, InvokeExpr ie, ConcurrentHashMap<String, String> ihaFiltered) {
        SootMethod cmet = getCallee(ie);
        if(cmet == null) {
            return;
        }
        // ['com.edimax.java.p2pJNI', 'jniCloseToU', ['int'], 'int']
        if (Utils.isComms(cmet)) {
//            log.info("[IHA]" + clx + "/" + met + " calls " + cmet.getDeclaringClass().getName() + "/" + cmet.getName());
            ihaFiltered.put(smet.getBytecodeSignature(), cmet.getSignature());
        }
    }

    public static void printArgTypes(List<Value> args) {
        String res = "";
        for (Value arg : args) {
            if (arg instanceof Local localArg) {
                log.info("Local: " + localArg.getType().toString());
            }
            else if (arg instanceof Constant constantArg) {
                log.info("Constant: " + constantArg.getType().toString());
            }
            else if (arg instanceof FieldRef fieldRef) {
                log.info("FieldRef: " + fieldRef.getField().getType().toString());
            }
            else if (arg instanceof Ref ref) {
                log.info("Ref: " + ref.getType().toString());
            }
        }
    }
        public static ArgumentAnalysisResult analyzeArgument(Value arg, Unit pt, Chain<Unit> units) {
            ArgumentAnalysisResult result = new ArgumentAnalysisResult();
            result.unit = pt.toString();
            if (arg instanceof Local) {
                Local localArg = (Local) arg;
                result.argumentName = localArg.getName();

                boolean start = false;
                for (Unit unit : units) {

                    // We only care about the reads and writes after the function call.
                    if (unit.equals(pt)) {
                        start = true;
                        continue;
                    }

                    if (start) {
                        if (unit.getUseBoxes().stream().anyMatch(useBox -> useBox.getValue().equals(localArg))) {
                            result.readCount++;
                        }
                        if (unit.getDefBoxes().stream().anyMatch(defBox -> defBox.getValue().equals(localArg))) {
                            result.writeCount++;
                        }
                    }
                }
            }
            else if (arg instanceof Constant) {
                result.argumentName = arg.toString();
            }
            else if (arg instanceof FieldRef fieldRef) {
                // This sets a field in the class may be used later in the apk so we need to track it.
                result.argumentName = fieldRef.getField().getName();
                result.writeCount++;
            }
            else if (arg instanceof ArrayRef arrayRef) {
                result.argumentName = arrayRef.toString();

                boolean start = false;
                for (Unit unit : units) {
                    if (unit.equals(pt)) {
                        start = true;
                        continue;
                    }

                    if (start) {
                        if (unit.getUseBoxes().stream().anyMatch(useBox -> useBox.getValue().equals(arrayRef))) {
                            result.readCount++;
                        }
                        if (unit.getDefBoxes().stream().anyMatch(defBox -> defBox.getValue().equals(arrayRef))) {
                            result.writeCount++;
                        }
                    }
                }
            }
            else {
                log.warning("Unhandled argument type: " + arg.getClass().getName());
            }
            return result;
        }
}
