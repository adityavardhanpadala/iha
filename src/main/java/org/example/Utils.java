package org.example;


import org.xmlpull.v1.XmlPullParserException;
import soot.SootMethod;
import soot.jimple.infoflow.InfoflowConfiguration;
import soot.jimple.infoflow.android.InfoflowAndroidConfiguration;
import soot.jimple.infoflow.android.manifest.ProcessManifest;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class Utils {

    private static List<String> netPkgs = Arrays.asList(
            "java.net.ContentHandler.getContent",
            "java.net.DatagramSocket.receive",
            "java.net.HttpURLConnection.getErrorStream",
            "java.net.HttpURLConnection.getHeaderField",
            "java.net.HttpURLConnection.getRequestMethod",
            "java.net.InetAddress",
            "java.net.MulticastSocket",
            "java.net.ServerSocket",
            "java.net.Socket",
            "java.net.URL",
            "java.net.URLConnection",
            "java.net.DatagramChannel",
            "java.net.SocketChannel",
            "java.net.ServerSocketChannel",
            "javax.net.HttpsURLConnection",
            "javax.net.SSLServerSocket",
            "javax.net.SSLSocket",
            "javax.net.SSLServerSocket",
            "javax.net.SSLServerSocketChannel",
            "javax.net.SSLSocketChannel",
            "javax.net.HttpsURLConnection",
            "javax.net.SSLEngine",
            "javax.net.SSLSession",
            "javax.net.SSLEngineResult",
            "javax.net.SSLContext",
            "javax.net.HttpsURLConnection",
            "android.net.ConnectivityManager",
            "android.net.NetworkInfo",
            "android.net.LinkProperties",
            "android.net.NetworkCapabilities",
            "android.net.UdpSocket",
            "android.net.ConnectivityManager",
            "android.net.Network",
            "android.net.UrlQuerySanitizer",
            "android.net.NetworkRequest",
            "android.webkit.WebView",
            "android.net.DhcpInfo",
            "android.webkit.WebViewClient",
            "android.webkit.WebViewDatabase",
            "android.bluetooth.BluetoothAdapter",
            "android.bluetooth.BluetoothSocket",
            "android.bluetooth.BluetoothServerSocket",
            "android.bluetooth.BluetoothDevice",
            "android.bluetooth.BluetoothGatt",
            "android.bluetooth.BluetoothGattCallback",
            "org.apache.");

    public static String getPackageName(String apkPath) {
        String packageName = "";
        try {
            ProcessManifest manifest = new ProcessManifest(apkPath);
            packageName = manifest.getPackageName();
        } catch (IOException | XmlPullParserException e) {
            e.printStackTrace();
        }
        return packageName;
    }

    public static boolean isAndroidMethod(SootMethod sootMethod){
        String clsSig = sootMethod.getDeclaringClass().getName();
        List<String> androidPrefixPkgNames = Arrays.asList("android.", "com.google.android", "androidx.","kotlinx.","kotlin.","okhttp3");
        return androidPrefixPkgNames.stream().map(clsSig::startsWith).reduce(false, (res, curr) -> res || curr);
    }

    public static boolean isComms(SootMethod sootMethod){
        if (sootMethod == null){
            return false;
        }
        String clsSig = sootMethod.getDeclaringClass().getName();
        return netPkgs.stream().map(clsSig::startsWith).reduce(false, (res, curr) -> res || curr);
    }

    public static boolean isWhiteListed(SootMethod sootMethod, List<String> whitelist){
        String clsSig = sootMethod.getDeclaringClass().getName();
        return whitelist.stream().map(clsSig::startsWith).reduce(false, (res, curr) -> res || curr);
    }
}
