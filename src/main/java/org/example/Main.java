package org.example;

import soot.*;
import soot.options.Options;
import soot.*;
import soot.jimple.*;
import soot.toDex.Debug;
import soot.util.Chain;

import javax.swing.plaf.synth.SynthStyle;
import java.awt.*;
import java.io.File;
import java.util.*;
import java.util.List;
import java.util.logging.*;
import java.util.Collections;

// Press Shift twice to open the Search Everywhere dialog and type `show whitespaces`,
// then press Enter. You can now see whitespace characters in your code.
public class Main {
    static Logger log = Logger.getLogger(Main.class.getName());

    public static void setupLogging() {
        // Add better formatting later.
        log.setLevel(Level.INFO);
    }

    public static void setupSoot(String[] args) {
        // Setup Soot.
        G.reset();
        Options.v().set_allow_phantom_refs(true);
        Options.v().set_whole_program(true);
        Options.v().set_prepend_classpath(true);
        Options.v().set_validate(true);
        Options.v().set_src_prec(Options.src_prec_apk);
        Options.v().set_output_format(Options.output_format_dex);
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

        System.out.println(args);
        setupLogging();
        setupSoot(args);

        Scene.v().loadNecessaryClasses();
        Scene.v().loadBasicClasses();
        Scene.v().loadDynamicClasses();

        List<SootMethod> ihaFiltered = new ArrayList<SootMethod>();

        HashSet<SootMethod> nativeAnalysisRequired = new HashSet<>();

        PackManager.v().getPack("jtp").add(new Transform("jtp.iha", new BodyTransformer() {
            @Override
            protected void internalTransform(Body body, String phaseName, Map<String, String> map) {
                if (Utils.isAndroidMethod(met)) {
                    return;
                }
                SootMethod smet = body.getMethod(); 
                String met = smet.getName();
                String clx = smet.getDeclaringClass().getName();

                Chain<Unit> units = body.getUnits();

                Map<SootMethod, InvokeExpr> filters = new HashMap<SootMethod, InvokeExpr>();

                for (Unit unit : units) {

                    if (unit instanceof InvokeStmt is) {
                        InvokeExpr ie = is.getInvokeExpr();
                        SootMethod cmet = getCallee(ie);
                        if (cmet.isNative()) {
                            nativeAnalysisRequired.add(smet)
                        }
                        if (Utils.isComms(cmet)) {
                            log.info("[IHA]" + clx + "/" + met + " calls " +
                                    cmet.getDeclaringClass().getName() + "/" + cmet.getName());
                            ihaFiltered.add(smet);
                        }
                    }

                    if (unit instanceof AssignStmt as) {
                        if (as.getRightOp() instanceof InvokeExpr ie) {
                            getCallee(ie);
                            SootMethod cmet = getCallee(ie);
                            if (cmet.isNative()) {
                                nativeAnalysisRequired.add(smet);
                            }

                            if (Utils.isComms(cmet)) {
                                log.info("[IHA]" + clx + "/" + met + " calls " +
                                        cmet.getDeclaringClass().getName() + "/" + cmet.getName());
                                ihaFiltered.add(smet);
                            }
                        }
                    }
                }
                // We don't need to validate since we are not modifying the body.
                // body.validate();
            }
        }));

        PackManager.v().runPacks();

        // nativeAnalysisRequired has all the methods that need to be analyzed 
        // further.
        
    }

}
