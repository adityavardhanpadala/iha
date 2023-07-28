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
import java.util.logging.Logger;
import java.util.Collections;

// Press Shift twice to open the Search Everywhere dialog and type `show whitespaces`,
// then press Enter. You can now see whitespace characters in your code.
public class Main {
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

    public static SootMethod getCallee(InvokeExpr ie){
        SootMethod met = null;

        if (ie instanceof VirtualInvokeExpr || ie instanceof InterfaceInvokeExpr) {
            // For virtual or interface invocations, use getMethodRef()
            SootMethodRef calleeMethodRef = ie.getMethodRef();
            SootMethod calleeMethod = calleeMethodRef.resolve();
            return calleeMethod;
        }
        else if (ie instanceof StaticInvokeExpr || ie instanceof SpecialInvokeExpr) {
            // For static or special invocations, use getMethod()
            SootMethod calleeMethod = ie.getMethod();
            return calleeMethod;
        }
        else {
            return met;
        }
    }
    public static void main(String[] args) {

        System.out.println("IHA lmao");
        setupSoot(args);

        Scene.v().loadNecessaryClasses();
        PackManager.v().getPack("jtp").add(new Transform("jtp.iha", new BodyTransformer() {
            @Override
            protected void internalTransform(Body body, String phaseName, Map<String, String> map) {
                if(Utils.isAndroidMethod(body.getMethod())){
                    return;
                }
                System.out.println("---------------------------------------------------------------------------");
                String met = body.getMethod().getName();
                String clx = body.getMethod().getDeclaringClass().getName();

                Chain<Unit> units = body.getUnits();

                Map<SootMethod, InvokeExpr> filters = new HashMap<SootMethod, InvokeExpr>();

                // Collect all the invokes from the body.
                for (Unit unit: units){
                    if(unit instanceof InvokeStmt is) {
                        InvokeExpr ie = is.getInvokeExpr();
                        System.out.println("[St InvokeExpr] " + ie);
                        SootMethod cmet = getCallee(ie);
                        if (Utils.isComms(cmet)) {
                            System.out.println("[IHA]" + cmet.getDeclaringClass().getName() + "--" + cmet.getName());
                        }
                    }

                    if(unit instanceof AssignStmt as) {
                         if (as.getRightOp() instanceof InvokeExpr ie){
                             System.out.println("[AS InvokeExpr] " + ie);
                             getCallee(ie);
                             SootMethod cmet = getCallee(ie);
                             if (Utils.isComms(cmet)) {
                                 System.out.println("[IHA]" + cmet.getDeclaringClass().getName() + "--" + cmet.getName());
                             }
                         }

                    }
                }

                // Check the function being invoked is in our whitelist of the functions.

                body.validate();
            }
        }));

        PackManager.v().runPacks();
//        PackManager.v().writeOutput();
    }

}