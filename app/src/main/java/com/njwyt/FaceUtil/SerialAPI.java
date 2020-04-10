package com.njwyt.FaceUtil;

import android.os.Handler;

import com.friendlyarm.AndroidSDK.HardwareControler;
import com.njwyt.AppContext;
import com.njwyt.content.Type;

/**
 * Created by zhongxuefei on 2017/2/19.
 */

public class SerialAPI {
    //初始化 串口3
    // private String devName = "/dev/s3c2410_serial3";
    private String devName = "/dev/ttySAC3";
    private int speed = 115200;
    private int dataBits = 8; //8位数据位
    private int stopBits = 1;//1位停止位
    private int devfd_seril = -1;   //设备读取失败（初始化）

    private final int BUFSIZE = 20;
    private byte[] buf = new byte[BUFSIZE];  //接收的数据
    private byte[] buf_send = new byte[BUFSIZE];//需要发送的数据

    private Handler mReadHandler;


    // SerialAPI(){}
    public int OpenSerialPort() {
        devfd_seril = -1;

        devfd_seril = HardwareControler.openSerialPort(devName, speed, dataBits, stopBits);

        //读取成功时，返回串口文件描述符
        if (devfd_seril >= 0) {
            //配置成功
            mReadHandler = new Handler();
            readSerial();
        } else {
            devfd_seril = -1;//配置失败
        }
        return devfd_seril;

    }

    public void CloseSerialPort() {

        if (devfd_seril != -1) {
            HardwareControler.close(devfd_seril);
            devfd_seril = -1;
        }

    }

    public void ReadSerialPort() {
        if (HardwareControler.select(devfd_seril, 0, 0) == 1) //有数据待读取
        {
            int retSize = HardwareControler.read(devfd_seril, buf, BUFSIZE);
            //读取成功返回读取的字节数
            if (retSize > 0) {
                if (buf[0] == '1')  //读取的是字符‘1’  返回上位机‘1’   即 发送数据为 接收的数据
                {
                    buf_send[0] = '1';
                    buf_send[1] = '\t';
                    buf_send[2] = '\n';
                    //HardwareControler.write(devfd_seril,buf_send);

                }
            }
        }
    }

    public void WriteSerialPort(String status) {
        ReadSerialPort();
        buf_send = new byte[BUFSIZE];
        status = status + "\t\n";
        buf_send = status.getBytes().clone();
        HardwareControler.write(devfd_seril, buf_send);
        buf_send = null;

        /*switch (status) {
            case "NORMAL":
                buf_send[0] = 'B';
                buf_send[1] = '0';
                buf_send[2] = '\t';
                buf_send[3] = '\n';
                HardwareControler.write(devfd_seril, buf_send);
                break;
            case "LIGHTUP":
                buf_send[0] = 'B';
                buf_send[1] = '1';
                buf_send[2] = '\t';
                buf_send[3] = '\n';
                HardwareControler.write(devfd_seril, buf_send);
                break;
            case "WARNING":
                buf_send[0] = 'B';
                buf_send[1] = '2';
                buf_send[2] = '\t';
                buf_send[3] = '\n';
                HardwareControler.write(devfd_seril, buf_send);
                break;
            case Type.DOOR_OPEN:
                buf_send[0] = 'P';
                buf_send[1] = '\t';
                buf_send[2] = '\n';
                HardwareControler.write(devfd_seril, buf_send);
                break;
            case Type.LIGHT_OFF:
                buf_send[0] = 'D';
                buf_send[1] = '\t';
                buf_send[2] = '\n';
                HardwareControler.write(devfd_seril, buf_send);
                break;
            case Type.LIGHT_ON:
                buf_send[0] = 'B';
                buf_send[1] = '\t';
                buf_send[2] = '\n';
                HardwareControler.write(devfd_seril, buf_send);
                break;
            default:
                break;
        }
        buf_send = null;*/
    }

    /**
     * 读取串口信息
     */
    private void readSerial() {

        mReadHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                if (HardwareControler.select(devfd_seril, 0, 0) == 1) {
                    int retSize = HardwareControler.read(devfd_seril, buf, BUFSIZE);
                    //读取成功返回读取的字节数
                    if (retSize > 0) {
                        String str = new String(buf);
                        System.out.println("-->> 接收到串口数据 = " + str);
                        str = str.substring(0, 1);
                        if (str.equals(Type.LIGHT_ON_RESULT)) {
                            AppContext.getInstance().openOutdoorActivity();
                        }
                    }
                }
                mReadHandler.postDelayed(this, 1000);
            }
        }, 200);
    }
}
