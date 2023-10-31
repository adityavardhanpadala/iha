package org.example;

import soot.*;
import soot.options.Options;
import soot.jimple.*;
import soot.util.Chain;

import java.util.*;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.*;
import java.util.Collections;
import java.util.logging.Formatter;

import static org.example.Utils.setupSoot;

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

        for(SootClass cls : Scene.v().getClasses()) {
            List<SootMethod> methods = new ArrayList<>(cls.getMethods());
            for(SootMethod smet : methods) {

                if(Utils.isAndroidMethod(smet)) {
                    continue;
                }

                Body body = null;
                try {
                    body = smet.retrieveActiveBody();
                } catch (Exception e) {
                    continue;
                }

                if(body == null) {
                    continue;
                }


                Chain<Unit> units = body.getUnits();
                for (Unit unit: units){
                    SootMethod cmet = null;
                    if(unit instanceof InvokeStmt is) {
                        InvokeExpr iex = is.getInvokeExpr();
                        cmet = analyzeCallee(smet, iex);
                    }
                    else if (unit instanceof AssignStmt) {
                        Value right = ((AssignStmt) unit).getRightOp();
                        if(right instanceof InvokeExpr iex) {
                            cmet = analyzeCallee(smet, iex);
                        }
                    }
                    if(cmet == null){
                        continue;
                    }
                    ihaFiltered.put(smet.getBytecodeSignature(), cmet.getBytecodeSignature());
                }
            }
        }

        for (Map.Entry<String, String> entry : ihaFiltered.entrySet()) {
            System.out.println("[IHA] " + entry.getKey() + "\t:\t" + entry.getValue());
        }
    }

    private static SootMethod analyzeCallee(SootMethod smet, InvokeExpr ie) {
        SootMethod cmet = getCallee(ie);

        if (Utils.isComms(cmet)) {
//            log.info("[IHA]" + smet.getDeclaringClass().getName() + "/" + smet.getName() + " calls " + cmet.getDeclaringClass().getName() + "/" + cmet.getName());
            return cmet;
        }
        else {
            return cmet;
        }
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
