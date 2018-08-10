package com.mai.udptest;

import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.TextView;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketTimeoutException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.Locale;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {

    private ArrayList<String> ipList;
    private TextView tv;
    private CheckBox logCheckBox;
    private boolean isRunning = false;
    private EditText ipListET;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        ipListET = findViewById(R.id.ipListEditText);
        tv = findViewById(R.id.textView);
        findViewById(R.id.testBtn).setOnClickListener(this);
        findViewById(R.id.clearBtn).setOnClickListener(this);
        logCheckBox = findViewById(R.id.logCheck);
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.testBtn:
                ipList = new ArrayList<>(Arrays.asList(ipListET.getText().toString().trim().split(";")));
                if (!isRunning)
                    startPing();
                appendLog("<--------start ping-------->");
                break;
            case R.id.clearBtn:
                tv.setText("");
                break;
        }
    }

    private void startPing() {
        appendLog("ipList.size() = " + ipList.size());

        final long[] startTime = new long[1];
        final long[] endTime = new long[1];
        final int[] successCount = {0};
        final int[] failureCount = {0};

        PingUtil instance = new PingUtil();
        instance.doPing(ipList, new PingResultListener() {
            @Override
            public void onStart(long l) {
                startTime[0] = l;
                isRunning = true;
                findViewById(R.id.testBtn).setClickable(false);
                findViewById(R.id.clearBtn).setClickable(false);
            }

            @Override
            public void onLog(String log) {
                if (logCheckBox.isChecked())
                    appendLog("log: " + log);
            }

            @Override
            public void onSuccess(String msg) {
                appendLog("<------------------------------------------>");
                appendLog("Success: rtt = " + msg.split(",")[0] + " packetAccess: " + msg.split(",")[1]);
                appendLog("<------------------------------------------>");
                successCount[0]++;
            }

            @Override
            public void onFailure(String msg) {
                appendLog("Failure: " + msg);
                failureCount[0]++;
            }

            @Override
            public void onEnd(long l) {
                findViewById(R.id.testBtn).setClickable(true);
                findViewById(R.id.clearBtn).setClickable(true);
                endTime[0] = l;
                isRunning = false;
                String endMsg = "Total ip count: " + ipList.size() + "\nSuccess count: " + successCount[0] + "\nFailure count: " + failureCount[0] + "\nIt takes: " + (endTime[0] - startTime[0]) + "ms";
                appendLog("<<<<<<<<<<<<<<<<<<<<<<<>>>>>>>>>>>>>>>>>>>>>>>");
                appendLog("<<<<<<<<<<<<<<<<<<<<<<<>>>>>>>>>>>>>>>>>>>>>>>");
                appendLog(endMsg);
                appendLog("<<<<<<<<<<<<<<<<<<<<<<<>>>>>>>>>>>>>>>>>>>>>>>");
                appendLog("<<<<<<<<<<<<<<<<<<<<<<<>>>>>>>>>>>>>>>>>>>>>>>");
                new AlertDialog.Builder(MainActivity.this).setTitle("测试结束").setMessage(endMsg).create().show();
            }
        });


    }

    private void appendLog(String log) {
        SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.CHINA);
        tv.append("[" + formatter.format(new Date()) + "] " + log + "\n");
    }

}

interface PingResultListener {
    void onStart(long l);

    void onLog(String log);

    void onSuccess(String msg);

    void onFailure(String msg);

    void onEnd(long l);
}

class PingUtil {

    private PingResultListener mListener;
    private final int MSG_START = 1000;
    private final int MSG_LOG = 1001;
    private final int MSG_SUCCESS = 1002;
    private final int MSG_FAILURE = 1003;
    private int messageCount = 0;
    private int msgCount = 0;

    private Handler mHandler = new Handler(Looper.getMainLooper()) {
        @Override
        public void handleMessage(Message msg) {
            String resultMsg = (String) msg.obj;
            switch (msg.what) {
                case MSG_START:
                    mListener.onStart(System.currentTimeMillis());
                    break;
                case MSG_LOG:
                    mListener.onLog(resultMsg);
                    break;
                case MSG_FAILURE:
                    msgCount++;
                    mListener.onFailure(resultMsg);
                    break;
                case MSG_SUCCESS:
                    msgCount++;
                    mListener.onSuccess(resultMsg);
                    break;
            }
            if (msgCount == ipList1.size()) {
                mListener.onEnd(System.currentTimeMillis());
                msgCount++;
            }
        }
    };
    private ArrayList<String> ipList1;

    public void doPing(ArrayList<String> ipList, PingResultListener pingListener) {
        ipList1 = ipList;
        Message startMsg = new Message();
        startMsg.what = MSG_START;
        mHandler.sendMessage(startMsg);

        mListener = pingListener;

        messageCount = ipList1.size();

        ThreadPoolExecutor threadPoolExecutor = new ThreadPoolExecutor(
                ipList1.size(),
                ipList1.size(),
                7000, TimeUnit.MILLISECONDS,
                new ArrayBlockingQueue<Runnable>(ipList1.size()));

        for (final String ip : ipList1) {
            threadPoolExecutor.execute(new Runnable() {
                @Override
                public void run() {
                    if (messageCount != 0) {
                        Message msg = new Message();
                        try {
                            String result = ping(ip.trim(), 3);
                            msg.what = MSG_SUCCESS;
                            msg.obj = result;
                        } catch (ArithmeticException e) {
                            msg.what = MSG_FAILURE;
                            msg.obj = e.toString();
                        } catch (IOException e) {
                            msg.what = MSG_FAILURE;
                            msg.obj = e.toString();
                        }
                        mHandler.sendMessage(msg);
                    }
                }
            });
        }
    }

    private String ping(String ip, int count) throws IOException {
        int resultCount = 0;
        long result = 0L;

        for (int i = 0; i < count; i++) {
            Message msg = new Message();
            msg.what = MSG_LOG;
            msg.obj = "Start send " + (i == 0 ? "first" : count == 1 ? "second" : "third") + " packet to server " + ip;
            mHandler.sendMessage(msg);
            try {
                result += ping(ip);
                resultCount++;
            } catch (SocketTimeoutException ignore) {
            }
        }

        if (resultCount == 0)
            throw new ArithmeticException();

        int avgRTT = (int) (result / resultCount);
        int packageLoss = (int) (((float) resultCount / (float) count) * 100);
        return avgRTT + "," + packageLoss;
    }

    private long ping(String ip) throws IOException {
        Message sendMsg = new Message();
        sendMsg.what = MSG_LOG;
        InetAddress address = InetAddress.getByName(ip);
        int port = 9999;
        // 2.创建数据报，包含发送的数据信息
        DatagramPacket packet = new DatagramPacket(new byte[0], 0, address, port);
        // 3.创建DatagramSocket对象
        DatagramSocket socket = new DatagramSocket();
        socket.setSoTimeout(2000);
        // 4.向服务器端发送数据报
        socket.send(packet);
        long startTime = System.currentTimeMillis();
        sendMsg.obj = "Send data to server " + address.toString() + ":" + port + " time: " + startTime;
        mHandler.sendMessage(sendMsg);

        /*
         * 接收服务器端响应的数据
         */
        // 1.创建数据报，用于接收服务器端响应的数据
        byte[] data2 = new byte[1024];
        DatagramPacket packet2 = new DatagramPacket(data2, data2.length);
        // 2.接收服务器响应的数据
        socket.receive(packet2);
        long endTime = System.currentTimeMillis();
        long l = endTime - startTime;
        Message receiveMsg = new Message();
        receiveMsg.what = MSG_LOG;
        receiveMsg.obj = "Receive data from server " + ip + " time: " + endTime + "\nItTakes: " + l + "ms...";
        mHandler.sendMessage(receiveMsg);
        // 4.关闭资源
        socket.close();
        return l;
    }
}