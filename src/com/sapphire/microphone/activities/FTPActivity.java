package com.sapphire.microphone.activities;

import android.app.Activity;
import android.os.Bundle;
import android.widget.CompoundButton;
import android.widget.Switch;
import android.widget.TextView;
import com.sapphire.microphone.MicrofonApp;
import com.sapphire.microphone.R;
import org.apache.ftpserver.FtpServer;
import org.apache.ftpserver.FtpServerFactory;
import org.apache.ftpserver.ftplet.Authority;
import org.apache.ftpserver.ftplet.FtpException;
import org.apache.ftpserver.ftplet.UserManager;
import org.apache.ftpserver.listener.ListenerFactory;
import org.apache.ftpserver.usermanager.PropertiesUserManagerFactory;
import org.apache.ftpserver.usermanager.impl.BaseUser;
import org.apache.http.conn.util.InetAddressUtils;

import java.io.File;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;


public class FTPActivity extends Activity {
    private FtpServer server = null;
    private static final int PORT = 2025;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.ftp_layout);
        final Switch switcher = (Switch) findViewById(R.id.switcher);
        final TextView ip = (TextView) findViewById(R.id.ip);
        switcher.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked) {
                    if (server == null) {
                        try {
                            startFTPServer();
                            ip.setText("ftp://1:1@" + getIPAddress(true) + ":" + PORT);
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    } else {
                        try {
                            server.start();
                            ip.setText("ftp://1:1@" + getIPAddress(true) + ":" + PORT);
                        } catch (FtpException e) {
                            e.printStackTrace();
                        }
                    }
                } else {
                    if (server != null) {
                        server.stop();
                        ip.setText("");
                    }
                }

            }
        });
    }

    private void startFTPServer() throws Exception {
        FtpServerFactory serverFactory = new FtpServerFactory();
        ListenerFactory factory = new ListenerFactory();
        factory.setPort(PORT);
        serverFactory.addListener("default", factory.createListener());
        factory.setImplicitSsl(false);
        PropertiesUserManagerFactory userManagerFactory = new PropertiesUserManagerFactory();
        final File f = new File(MicrofonApp.getAppDir(), "myusers.properties");
        if (!f.exists())
            f.createNewFile();
        userManagerFactory.setFile(f);
        BaseUser user = new BaseUser();
        user.setName("1");
        user.setPassword("1");
        user.setHomeDirectory(MicrofonApp.getAppDir().getAbsolutePath());
        List<Authority> authorities = new ArrayList<Authority>();
        user.setAuthorities(authorities);
        UserManager um = userManagerFactory.createUserManager();
        try {
            um.save(user);
        } catch (FtpException e) {
            e.printStackTrace();
        }
        serverFactory.setUserManager(userManagerFactory.createUserManager());
        server = serverFactory.createServer();
        server.start();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (server != null)
            server.stop();
    }

    public static String getIPAddress(boolean useIPv4) {
        try {
            List<NetworkInterface> interfaces = Collections.list(NetworkInterface.getNetworkInterfaces());
            for (NetworkInterface intf : interfaces) {
                List<InetAddress> addrs = Collections.list(intf.getInetAddresses());
                for (InetAddress addr : addrs) {
                    if (!addr.isLoopbackAddress()) {
                        String sAddr = addr.getHostAddress().toUpperCase();
                        boolean isIPv4 = InetAddressUtils.isIPv4Address(sAddr);
                        if (useIPv4) {
                            if (isIPv4)
                                return sAddr;
                        } else {
                            if (!isIPv4) {
                                int delim = sAddr.indexOf('%');
                                return delim < 0 ? sAddr : sAddr.substring(0, delim);
                            }
                        }
                    }
                }
            }
        } catch (Exception ignored) {}
        return "127.0.0.1";
    }
}
