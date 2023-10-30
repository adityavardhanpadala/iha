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

import static org.example.Main.log;

public class Utils {

    private static List<String> netPkgs = Arrays.asList(
            "java.net.CacheResponse: getHeaders",
            "java.net.CacheResponse: getBody",

            "java.net.ContentHandler: getContent",

            "java.net.CookieHandler: get",

            "java.net.DatagramPacket: getData",
            "java.net.DatagramPacket: getLength",
            "java.net.DatagramPacket: getPort",
            "java.net.DatagramPacket: getAddress",
            "java.net.DatagramPacket: getSocketAddress",

            "java.net.DatagramSocket: receive",
            "java.net.DatagramSocket: getReceiveBufferSize",

            "java.net.HttpURLConnection: getErrorStream",
            "java.net.HttpURLConnection: getHeaderField",
            "java.net.HttpURLConnection: getRequestMethod",
            "java.net.HttpURLConnection: getResponseCode",
            "java.net.HttpURLConnection: getResponseMessage",
            "java.net.HttpURLConnection: getContent",
            "java.net.HttpURLConnection: getContentLength",
            "java.net.HttpURLConnection: getHeaderField",
            "java.net.HttpURLConnection: getHeaderFields",
            "java.net.HttpURLConnection: getInputStream",
            "java.net.HttpURLConnection: guessContentTypeFromName",
            "java.net.HttpURLConnection: guessContentTypeFromStream",

            "java.net.URLConnection: getErrorStream",
            "java.net.URLConnection: getHeaderField",
            "java.net.URLConnection: getRequestMethod",
            "java.net.URLConnection: getResponseCode",
            "java.net.URLConnection: getResponseMessage",
            "java.net.URLConnection: getContent",
            "java.net.URLConnection: getContentLength",
            "java.net.URLConnection: getHeaderField",
            "java.net.URLConnection: getHeaderFields",
            "java.net.URLConnection: getInputStream",
            "java.net.URLConnection: guessContentTypeFromName",
            "java.net.URLConnection: guessContentTypeFromStream",

            "java.net.HttpCookie: getName",
            "java.net.HttpCookie: getValue",

            "java.net.InetAddress: getAddress",

            "java.net.MulticastSocket: receive",
            "java.net.MulticastSocket: getReceiveBufferSize",

            "java.net.ResponseCache: get",

            "java.net.ServerSocket: receive", // wait for gpt

            "java.net.Socket: getInputStream",
            "java.net.Socket: getOutputStream",
            "java.net.Socket: getReceiveBufferSize",

            // Skip java.net.URI

            // Do we skip below since these are subclasses of other frequently used classes?
            // "java.net.URL",
            // "java.net.URLConnection",
            // "java.net.URLStreamHandler",

            // Ignored
            // "java.net.DatagramChannel",

            // Ignoring certificate based methods and just using the methods that handle
            // data.
            "javax.net.ssl.HttpURLConnection: getErrorStream",
            "javax.net.ssl.HttpURLConnection: getHeaderField",
            "javax.net.ssl.HttpURLConnection: getRequestMethod",
            "javax.net.ssl.HttpURLConnection: getResponseCode",
            "javax.net.ssl.HttpURLConnection: getResponseMessage",
            "javax.net.ssl.HttpURLConnection: getContent",
            "javax.net.ssl.HttpURLConnection: getContentLength",
            "javax.net.ssl.HttpURLConnection: getHeaderField",
            "javax.net.ssl.HttpURLConnection: getHeaderFields",
            "javax.net.ssl.HttpURLConnection: getInputStream",
            "javax.net.ssl.HttpURLConnection: guessContentTypeFromName",
            "javax.net.ssl.HttpURLConnection: guessContentTypeFromStream",

            "javax.net.ssl.SSLServerSocket: receive",
            "javax.net.ssl.SSLServerSocket: getReceiveBufferSize",

            "javax.net.ssl.SSLSocket: getInputStream",
            "javax.net.ssl.SSLSocket: getReceiveBufferSize",
            //
            // "javax.net.ssl.SSLServerSocket",
            // "javax.net.ssl.SSLServerSocketChannel",
            // "javax.net.ssl.SSLSocketChannel",
            // "javax.net.ssl.HttpsURLConnection",
            // "javax.net.ssl.SSLEngine",
            // "javax.net.ssl.SSLSession",
            // "javax.net.ssl.SSLEngineResult",
            // "javax.net.ssl.SSLContext",

            "android.net.wifi.ScanResult: getWifiSsid",
            "android.net.wifi.WifiInfo: getSSID",
            "android.net.wifi.WifiInfo: getBSSID",
            "android.net.wifi.WifiInfo: getIpAddress",
            "android.net.wifi.WifiInfo: getMacAddress",

            "android.net.wifi.WifiManager: getConnectionInfo",
            "android.net.wifi.WifiManager: getScanResults",
            "android.net.wifi.WifiManager: getDhcpInfo",

            // Ignoring? check later based on results from IHA stats.
            // "android.net.ConnectivityManager",

            "android.net.LocalSocket: getInputStream",
            "android.net.LocalSocket: getOutputStream",
            "android.net.LocalSocket: getReceiveBufferSize",

            // Ignored for now. Check later based on results from IHA stats.
            // "android.net.NetworkInfo",
            // "android.net.LinkProperties",
            // "android.net.NetworkCapabilities",
            // "android.net.ConnectivityManager",
            // "android.net.Network",

            "android.net.NetworkRequest: getCapabilities",

            // "android.webkit.WebView", this is a sink idiot
            // "android.net.DhcpInfo",
            // "android.webkit.WebViewClient",
            // "android.webkit.WebViewDatabase",
            "android.bluetooth.BluetoothDevice: getAddress",
            "android.bluetooth.BluetoothDevice: getName",
            "android.bluetooth.BluetoothDevice: getUuids",

            "android.bluetooth.BluetoothSocket: getInputStream",
            "android.bluetooth.BluetoothSocket: getOutputStream",

            // Ignoring since it creates a BT socket.
            // "android.bluetooth.BluetoothServerSocket",
            "android.bluetooth.BluetoothGatt: readCharacteristic",
            "android.bluetooth.BluetoothGatt: readDescriptor",

            // TODO: analyse how the request is being used.
            "okhttp3.OkHttpClient.newCall: execute",

            // TODO: in case of enqueue, the analysis should determine the
            // data dependency of the reponse and mark the functions accordingly.
            // Cases like this is where we taint the field. Check mariana trench docs .
            "okhttp3.OkHttpClient.newCall: enqueue"

    // "org.apache.http",
    // "org.apache.log4j" sinks

    );

    // TODO: Check MTP and media.tv packages.
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

    public static boolean isAndroidMethod(SootMethod sootMethod) {
        String clsSig = sootMethod.getDeclaringClass().getName();
        List<String> androidPrefixPkgNames = Arrays.asList("android.", "com.google.android", "androidx.", "kotlinx.",
                "kotlin.", "okhttp3");
        return androidPrefixPkgNames.stream().map(clsSig::startsWith).reduce(false, (res, curr) -> res || curr);
    }

    public static boolean isComms(SootMethod sootMethod) {
        if (sootMethod == null) {
            return false;
        }
    
        String clsName = sootMethod.getDeclaringClass().getName();
        String metName = sootMethod.getName();
        String signature = clsName + ": " + metName;
        
        for (String netPkg : netPkgs) {
            if (signature.contains(netPkg) | netPkg.contains(signature)) {
                return true;
            }
        }
        return false;
    }

    public static boolean isWhiteListed(SootMethod sootMethod, List<String> whitelist) {
        String clsSig = sootMethod.getDeclaringClass().getName();
        return whitelist.stream().map(clsSig::startsWith).reduce(false, (res, curr) -> res || curr);
    }
}
