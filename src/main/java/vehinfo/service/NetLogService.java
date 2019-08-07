package vehinfo.service;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import vehinfo.entity.ConnectInfo;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.*;

@Service
public class NetLogService {
    private Log log = LogFactory.getLog(NetLogService.class);
    private ThreadPoolExecutor threadPoolExecutor =
            new ThreadPoolExecutor(2, 10, 60, TimeUnit.SECONDS, new SynchronousQueue<>());
    enum READ_STATUS {
        READ_HEAD,
        READ_LENGTH,
        READ_BODY
    }
    private ServerSocket serverSocket;

    private String park;

    @Value("${tcp.port}")
    private int port;

    private List<ConnectInfo> connectInfoList = new ArrayList<>();

    final private String HEAD = "kunyan123";
    final private String END = "221data";
    final int LENGTH = 4;

    @PostConstruct
    private void init() {
        threadPoolExecutor.execute(new Runnable() {
            @Override
            public void run() {
                startService();
            }
        });
    }

    boolean readNbyte(InputStream is, int len, ByteArrayOutputStream byteArrayOutputStream) {
        try {
            int n = 0;
            int total = 0;
            if (is.available() < len) {
                return false;
            }
            byte [] buf = new byte[100];
            while (total < len) {
                n = is.read(buf , 0, len > 100 ? 100 : len);
                if (n >= 0) {
                    total += n;
                    byteArrayOutputStream.write(buf, 0, n);
                } else {
                    return false;
                }
            }
            return true;
        } catch (IOException e) {
            return false;
        }
    }

    boolean readHead(InputStream is, ByteArrayOutputStream byteArrayOutputStream) {
       boolean f =  readNbyte(is, HEAD.length(), byteArrayOutputStream);
       if (!f) {
           return f;
       }
       String s = new String(byteArrayOutputStream.toByteArray());
       if (s.equals(HEAD)) {
           return true;
       }
       return false;
    }

    private int readLength(InputStream is, ByteArrayOutputStream byteArrayOutputStream) {
        boolean f =  readNbyte(is, LENGTH, byteArrayOutputStream);
        if (!f) {
            return -1;
        }
        String s = new String(byteArrayOutputStream.toByteArray());
        int len = Integer.parseInt(s);
        return len;
    }

    private boolean readBody(InputStream is, int len, ByteArrayOutputStream byteArrayOutputStream) {
        boolean f =  readNbyte(is, len, byteArrayOutputStream);
        if (!f) {
            return f;
        }
        String s = new String(byteArrayOutputStream.toByteArray());
        return true;
    }

    void readSocket(Socket socket) {

        READ_STATUS status = READ_STATUS.READ_HEAD;
        ByteArrayOutputStream buf = new ByteArrayOutputStream();
        InputStream is = null;
        try {
            is = socket.getInputStream();
        } catch (IOException e) {
            log.error("get stream error:" + e.getMessage());
        }
        int len = -1;
        int timeout = 60;
        long timeStamp = System.currentTimeMillis() / 1000;
        while (true) {
            try {
                if (System.currentTimeMillis() / 1000 - timeStamp > timeout) {
                    socket.close();
                    log.error("timeout close:" + socket.getInetAddress() + socket.getPort());
                    return;
                }
                switch (status) {
                    case READ_HEAD:
                        if (readHead(is, buf)) {
                            status = READ_STATUS.READ_LENGTH;
                            buf.reset();
                            timeStamp = System.currentTimeMillis() / 1000;
                        } else {
                            try {
                                Thread.sleep(50);
                            } catch (InterruptedException e) {
                            }
                        }
                        break;
                    case READ_LENGTH:
                        len = readLength(is, buf);
                        if (len != -1) {
                            if (len > 1000) {
                                status = READ_STATUS.READ_HEAD;
                            } else {
                                status = READ_STATUS.READ_BODY;
                            }
                            buf.reset();
                        }
                        break;
                    case READ_BODY:
                        if (readBody(is, len, buf)) {
                            status = READ_STATUS.READ_HEAD;
                            String body = buf.toString();
                            if (!body.startsWith("aaaa")) {
                                park = body;
                            }
                            buf.reset();
                        }
                        break;
                }
            } catch (IOException e) {
                log.error("io exception:" + e.getMessage());
            }
        }
    }

    public String getPark() {
        return park;
    }

    public void setPark(String park) {
        this.park = park;
    }

    private void startService() {
        try {
            log.info("bind server port:" + port);
            serverSocket = new ServerSocket(port, 10);
        }catch (IOException e) {
           log.error("bind server port error:" + e.getMessage());
        }

        while (true) {
            Socket connSocket = null;
                try {
                    connSocket = serverSocket.accept();
                }catch (IOException e) {
                    log.error("accept error:" + e.getMessage());
                    return;
                }
                final  Socket socket = connSocket;
                log.info("New connection accepted " + socket.getInetAddress() + ":" + socket.getPort());
                final ConnectInfo connectInfo = new ConnectInfo();
                connectInfo.setConnTime(new Date());
                connectInfo.setIp(socket.getInetAddress().getHostAddress());
                connectInfo.setPort(socket.getPort());
                synchronized (this) {
                    connectInfoList.add(connectInfo);
                }
                threadPoolExecutor.execute(new Runnable() {
                    public void run() {
                        try {
                            readSocket(socket);
                        } catch (Exception e) {
                            log.error("read socket for " + socket.getInetAddress() + " error  " + e.getMessage());
                        } finally {
                            synchronized (this) {
                                connectInfoList.remove(connectInfo);
                            }
                            try {
                                log.info("close socket :" + socket.getInetAddress());
                                socket.close();
                            } catch (IOException e1) {
                                log.error("close sock error:" + e1.getMessage());
                            }
                        }
                    }
                });
        }
    }

    public List<ConnectInfo> getConnectInfoList() {
        List<ConnectInfo> arrayList = new ArrayList<>();
        synchronized (this) {
            arrayList = new ArrayList<>(connectInfoList);
        }
        return arrayList;
    }

    public int getActiveSize() {
        return threadPoolExecutor.getActiveCount();
    }

    public int getCoreSize() {
        return threadPoolExecutor.getCorePoolSize();
    }

    public long getTaskCount() {
        return threadPoolExecutor.getTaskCount();
    }

    public long getLargestSize() {
        return threadPoolExecutor.getLargestPoolSize();
    }


}
