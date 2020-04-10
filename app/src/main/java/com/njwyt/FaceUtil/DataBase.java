package com.njwyt.FaceUtil;

import android.graphics.Bitmap;
import android.util.Log;

import com.njwyt.content.Address;

import org.opencv.android.Utils;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;

import static android.R.attr.name;

/**
 * Created by zx on 2016/11/1.
 */
public class DataBase {
    public int _sample_num;
    public int _people_num;
    public Mat _labels=new Mat();
    public boolean SYSTEM_INIT=true;
    public List<Mat> _src=new ArrayList<Mat>();
    public List<String> _name=new ArrayList<String>();
    public List<String> _people=new ArrayList<String>();
    public List<Integer> _label_array=new ArrayList<Integer>();
    public List<String> _date=new ArrayList<String>();
    public List<Integer> _nth=new ArrayList<Integer>();
    public List<Integer> _rec_ratio=new ArrayList<Integer>();
    public boolean _database_status=false;
    public DataBase(){};
    public void clear()
    {
        _sample_num=0;
        if(!_labels.empty())_labels.release();
        if(!_src.isEmpty())_src.clear();
        if(!_name.isEmpty())_name.clear();
        if(!_label_array.isEmpty())_label_array.clear();
        if(!_date.isEmpty())_date.clear();
        if(!_nth.isEmpty())_nth.clear();
        if(!_rec_ratio.isEmpty())_rec_ratio.clear();
        _database_status=false;
    }
    public boolean read_dbase_from_image()
    {
        clear();
        File fis=new File(Address.DATABASE);
        if(!fis.exists())
            return false;
        else{
            File f = null;
            File[] paths;

            try{
                // create new file
                f = new File(Address.DATABASE);

                // create new filename filter
                FilenameFilter fileNameFilter = new FilenameFilter() {

                    @Override
                    public boolean accept(File dir, String name) {
                        if(name.lastIndexOf('.')>0)
                        {
                            // get last index for '.' char
                            int lastIndex = name.lastIndexOf('.');

                            // get extension
                            String str = name.substring(lastIndex);

                            // match path name extension
                            if(str.equals(".jpg")||str.equals(".bmp"))
                            {
                                return true;
                            }
                        }
                        return false;
                    }
                };
                // returns pathnames for files and directory
                paths = f.listFiles(fileNameFilter);
                int curLabel = 0,index_name,index_date,index_nth,index_rec_ratio;
                String str,name,date,nth,rec_ratio;
                Mat temp_img;
                // for each pathname in pathname array
                if(paths.length==0)
                    return false;
                else {
                    for (File path : paths) {
                        // prints file and directory paths
                        str = path.getName().toString();//name_date_nth_recratio.jpg
                        index_name = str.indexOf("_");
                        index_date = str.indexOf("_", index_name + 1);
                        index_nth = str.indexOf("_", index_date + 1);
                        index_rec_ratio = str.indexOf(".", index_nth + 1);
                        name = str.substring(0, index_name);
                        date = str.substring(index_name + 1, index_date);
                        nth = str.substring(index_date + 1, index_nth);
                        rec_ratio = str.substring(index_nth + 1, index_rec_ratio);
                        _sample_num++;
                        _label_array.add(curLabel++);
                        _name.add(name);
                        _date.add(date);
                        _nth.add(Integer.valueOf(nth));
                        _rec_ratio.add(Integer.valueOf(rec_ratio));
                        temp_img = Imgcodecs.imread(path.toString());
                        Mat gray_img = new Mat();
                        Imgproc.cvtColor(temp_img, gray_img, Imgproc.COLOR_BGR2GRAY);
                        _src.add(gray_img);
                        System.out.println(path);
                    }
                    Integer[] lbs = new Integer[_label_array.size()];
                    byte[] test = new byte[_label_array.size()];
                    _label_array.toArray(lbs);
                    for (int is = 0; is < _label_array.size(); is++)
                        test[is] = (byte) (lbs[is] & 0xff);
                    _labels = new Mat(_label_array.size(), 1, CvType.CV_8UC1);
                    _labels.put(0, 0, test);
                    _database_status = true;
                }
            }catch(Exception e){
                // if any error occurs
                e.printStackTrace();
            }
        }
        return true;

    }
    public boolean write_dbase_to_image()
    {
        String path= Address.DATABASE;
        String filename;
        File fis=new File(path);
        Bitmap facebitmap = null;

        if(!fis.exists())
            fis.mkdir();
        else {
            for(int i=0;i<_sample_num;i++)
            {
                filename=path+_name.get(i)+"_"+_date.get(i)+"_"+_nth.get(i)+"_"+_rec_ratio.get(i)+".jpg";
                try {
                    Mat faceImage=new Mat();
                    _src.get(i).copyTo(faceImage);
                    Imgproc.cvtColor(faceImage, faceImage, Imgproc.COLOR_GRAY2BGR);
                    facebitmap = Bitmap.createBitmap(faceImage.cols(), faceImage.rows(), Bitmap.Config.ARGB_8888);
                    Utils.matToBitmap(faceImage, facebitmap);
                    if(facebitmap!=null) {
                        BufferedOutputStream bos = new BufferedOutputStream(new FileOutputStream(filename));
                        facebitmap.compress(Bitmap.CompressFormat.JPEG, 100, bos);//将图片压缩的流里面
                        bos.flush();// 刷新此缓冲区的输出流
                        bos.close();// 关闭此输出流并释放与此流有关的所有系统资源
                        if (!facebitmap.isRecycled()) facebitmap.recycle();//回收bitmap空间
                    }
                } catch (Exception e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
            }
        }
        return true;
    }
    public boolean write_dbase()
    {

        String path=Address.DATABASE;
        String filename,output;
        File fis=new File(path);
        if(!fis.exists())fis.mkdir();
        if(fis.exists()){
            filename=path+"data.bin";
            try {
                BufferedOutputStream bos = new BufferedOutputStream(new FileOutputStream(filename));
                bos.write(_sample_num);
                for(int i=0;i<_sample_num;i++)
                {
                    output=_name.get(i)+"_"+_date.get(i)+"_"+_nth.get(i)+"_"+_rec_ratio.get(i)+".jpg";
                    bos.write(i);
                    byte[] out=output.getBytes();
                    bos.write(output.getBytes().length);
                    bos.write(output.getBytes());
                    Mat faceImage=_src.get(i);
                    //Imgproc.cvtColor(faceImage, faceImage, Imgproc.COLOR_GRAY2BGR);
                    byte[] imgdata=new byte[faceImage.rows()*faceImage.cols()];
                    faceImage.get(0,0,imgdata);
                    bos.write(faceImage.rows());
                    bos.write(faceImage.cols());
                    bos.write(imgdata);
                }
            } catch (Exception e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
                return false;
            }
        }
        return true;
    }
    public boolean delete_item(String name_tobe_deleted)
    {
        if(_database_status)
        {
            for(int i=0;i<_sample_num;i++)
            {
                if(_name.get(i).equalsIgnoreCase(name_tobe_deleted)){
                    _label_array.remove(i);
                    _name.remove(i);
                    _date.remove(i);
                    _nth.remove(i);
                    _rec_ratio.remove(i);
                    _src.remove(i);
                    i--;
                    _sample_num--;
                }
            }
            _people = new ArrayList<String>(new HashSet<String>(_name));//去除重名
            _people_num=_people.size();

            Integer[] lbs=new Integer[_label_array.size()];
            byte[] test=new byte[_label_array.size()];
            _label_array.toArray(lbs);
            for(int is=0;is<_label_array.size();is++)
                test[is]= (byte) (lbs[is]&0xff);
            _labels=new Mat(_label_array.size(),1,CvType.CV_8UC1);
            _labels.put(0,0,test);
        }
        write_dbase();
        return true;
    }
    public boolean modify_item(String name_tobe_deleted, String newname)
    {
        if(_database_status)
        {
            for(int i=0;i<_sample_num;i++)
            {
                if(_name.get(i).equalsIgnoreCase(name_tobe_deleted)){
                    _name.set(i,newname);
                }
            }
            _people = new ArrayList<String>(new HashSet<String>(_name));//去除重名
            _people_num=_people.size();

            Integer[] lbs=new Integer[_label_array.size()];
            byte[] test=new byte[_label_array.size()];
            _label_array.toArray(lbs);
            for(int is=0;is<_label_array.size();is++)
                test[is]= (byte) (lbs[is]&0xff);
            _labels=new Mat(_label_array.size(),1,CvType.CV_8UC1);
            _labels.put(0,0,test);
        }
        write_dbase();
        return true;
    }

    public boolean read_dbase()
    {
        clear();
        String path=Address.DATABASE;
        String filename=path+"data.bin";
        String input;
        _people_num=0;
        File fis=new File(filename);
        if(!fis.exists()){
            SYSTEM_INIT=true;
            return false;
        }
        else {
            SYSTEM_INIT=false;
            try {
                BufferedInputStream bis = new BufferedInputStream(new FileInputStream(filename));
                _sample_num=bis.read();
                if(_sample_num<=0) {
                    return false;
                }
                for(int i=0;i<_sample_num;i++)
                {
                    int isample_nth=bis.read();
                    int item_name_lenth=bis.read();
                    byte[]  item_name=new byte[item_name_lenth];
                    bis.read(item_name);
                    int image_height=bis.read();
                    int image_width=bis.read();
                    byte[] data=new byte[image_height*image_width];
                    //byte[] data=new byte[image_height*image_width*3];
                    bis.read(data);
                    //Mat faceImage=new Mat(image_height,image_width, CvType.CV_8UC3);
                    Mat faceImage=new Mat(image_height,image_width, CvType.CV_8UC1);
                    faceImage.put(0,0,data);

                    int index_name,index_date,index_nth,index_rec_ratio;
                    String str,name,date,nth,rec_ratio;
                    str=new String(item_name);
                    index_name = str.indexOf("_");
                    index_date = str.indexOf("_", index_name + 1);
                    index_nth = str.indexOf("_", index_date + 1);
                    index_rec_ratio = str.indexOf(".", index_nth + 1);
                    name = str.substring(0, index_name);
                    date = str.substring(index_name + 1, index_date);
                    nth = str.substring(index_date + 1, index_nth);
                    rec_ratio = str.substring(index_nth + 1, index_rec_ratio);

                    _label_array.add(isample_nth);
                    _name.add(name);
                    _date.add(date);
                    _nth.add(Integer.valueOf(nth));
                    _rec_ratio.add(Integer.valueOf(rec_ratio));
                    _src.add(faceImage);
                    //Imgproc.cvtColor(faceImage, faceImage, Imgproc.COLOR_GRAY2BGR);
                    //Bitmap facebitmap = Bitmap.createBitmap(faceImage.cols(), faceImage.rows(), Bitmap.Config.ARGB_8888);
                    //Utils.matToBitmap(faceImage, facebitmap);

                }
            } catch (Exception e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
            _people = new ArrayList<String>(new HashSet<String>(_name));//去除重名
            _people_num=_people.size();

            Integer[] lbs=new Integer[_label_array.size()];
            byte[] test=new byte[_label_array.size()];
            _label_array.toArray(lbs);
            for(int is=0;is<_label_array.size();is++)
                test[is]= (byte) (lbs[is]&0xff);
            _labels=new Mat(_label_array.size(),1,CvType.CV_8UC1);
            _labels.put(0,0,test);
            _database_status=true;
        }
        return true;
    }

    /**
     * 更新人脸识别数据库
     * @param name
     * @param sample_th
     * @param rec_ratio
     * @param faceImage
     */
    public void update_database(String name,int sample_th,int rec_ratio,Mat faceImage)
    {
        if(_database_status||SYSTEM_INIT)
        {
            if(rec_ratio>0) { //new User 模式时，调用参数的sample_th和rec_ratio均设为0，此时进入else部分直接修改数据
                _rec_ratio.set(sample_th, rec_ratio);
            }
            else {
                Date rightNow = new Date();
                SimpleDateFormat Format = new SimpleDateFormat("yyyy-MM-dd hh:mm:ss");
                String now = Format.format(rightNow);
                int min_rec_ratio = 1000;
                int min_rec_ratio_item = -1;
                int match_name_num = 0;
                for (int i = 0; i < _name.size(); i++) {//搜索名字为name的人的照片数量和匹配率最低值和数据index
                    if (_name.get(i).equals(name)) {
                        if (_rec_ratio.get(i) < min_rec_ratio) {
                            min_rec_ratio_item = i;
                            min_rec_ratio = _rec_ratio.get(i);
                        }
                        match_name_num++;
                    }
                }
                if (match_name_num == 30)//已有30个数据则替换掉识别率最小的那个
                {
                    int nth = min_rec_ratio_item;
                    _src.set(nth, faceImage);
                    _date.set(nth, now);
                    _rec_ratio.set(nth, 1);
                }
                else {  //新增数据
                    _name.add(name);
                    _src.add(faceImage);
                    _date.add(now);
                    _nth.add(match_name_num);
                    _rec_ratio.add(0);
                    _sample_num++;
                }
                    //_label_array.remove(nth);//因为是按序排列的,因此样本满了之后不用remove
            }
            try {
                Thread.sleep(500);
                write_dbase();
                //write_dbase_to_image();
            } catch (Exception e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }

    }
}
