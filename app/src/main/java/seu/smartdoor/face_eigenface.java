package seu.smartdoor;

import android.graphics.Bitmap;

import com.njwyt.FaceUtil.DataBase;

import org.opencv.android.Utils;
import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.Size;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;

import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

/**
 * Created by zx on 2016/9/26.
 */
public class face_eigenface {
    private int _num_components;
    private double _threshold;
    private List<Mat> _projections = new ArrayList<Mat>();
    ;
    private Mat _eigenvectors = new Mat();
    private Mat _eigenvalues = new Mat();
    private Mat _mean = new Mat();
    public Mat _labels = new Mat();
    public List<Mat> _src = new ArrayList<Mat>();
    public List<String> _name = new ArrayList<String>();
    public List<String> _people = new ArrayList<String>();
    public List<Integer> _label_array = new ArrayList<Integer>();
    public List<String> _date = new ArrayList<String>();
    public List<Integer> _nth = new ArrayList<Integer>();
    public List<Integer> _rec_ratio = new ArrayList<Integer>();
    public boolean _database_status = false;
    public int _match_ratio;
    public DataBase dataBase = new DataBase();
    public int _people_num;

    /*   static {
           System.loadLibrary("eigenface");
       }*/
    public void clear() {
        if (!_projections.isEmpty()) _projections.clear();
        if (!_eigenvalues.empty()) _eigenvalues.release();
        if (!_eigenvectors.empty()) _eigenvectors.release();
        if (!_mean.empty()) _mean.release();
        if (!_labels.empty()) _labels.release();
        if (!_src.isEmpty()) _src.clear();
        if (!_name.isEmpty()) _name.clear();
        if (!_people.isEmpty()) _people.clear();
        if (!_label_array.isEmpty()) _label_array.clear();
        if (!_date.isEmpty()) _date.clear();
        if (!_nth.isEmpty()) _nth.clear();
        if (!_rec_ratio.isEmpty()) _rec_ratio.clear();
        _people_num = 0;
        _database_status = false;
    }

    public face_eigenface(int num_components, double threshold) {
        _num_components = num_components;
        _threshold = threshold;
        if (_database_status) clear();

        /*connPool
                = new ConnectionPool("org.postgresql.Driver"
                ,"jdbc:postgresql://dbURI:5432/DBName"
                ,"postgre"
                ,"postgre");
        try{
            connPool.createPool();
            Connection conn = connPool.getConnection();
        }
        catch (java.lang.Exception e)
        {
            System.out.println("数据库连接失败");
        }*/
    }

    face_eigenface(List<Mat> src, Mat labels, int num_components, double threshold) {
        _num_components = num_components;
        _threshold = threshold;
        /*connPool
                = new ConnectionPool("org.postgresql.Driver"
                ,"jdbc:postgresql://dbURI:5432/DBName"
                ,"postgre"
                ,"postgre");

        try{
            connPool.createPool();
            Connection conn = connPool.getConnection();
            train();
        }
        catch (java.lang.Exception e)
        {
            System.out.println("数据库连接失败");
        }*/

    }

    // Computes an Eigenfaces model with images in src and corresponding labels
    // in labels.
    public boolean read_dbase_from_image() {
        /*File fis=new File("/sdcard/myImage/Database/");
        if(!fis.exists())
            return false;
        else{
            File f = null;
            File[] paths;

            try{
                // create new file
                f = new File("/sdcard/myImage/Database/");

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


        }*/
        boolean result = dataBase.read_dbase_from_image();
        _labels = dataBase._labels;
        _src = dataBase._src;
        _name = dataBase._name;
        _people = dataBase._people;
        _label_array = dataBase._label_array;
        _date = dataBase._date;
        _nth = dataBase._nth;
        _rec_ratio = dataBase._rec_ratio;
        _database_status = true;
        return true;

    }

    public boolean write_dbase_img() {
        String path = "/sdcard/myImage/Database/";
        String filename;
        File fis = new File(path);
        Bitmap facebitmap = null;

        if (!fis.exists())
            fis.mkdir();
        else {
            for (int i = 0; i < _labels.size().height; i++) {
                filename = path + _name.get(i) + "_" + _date.get(i) + "_" + _nth.get(i) + "_" + _rec_ratio.get(i) + ".jpg";
                try {
                    Mat faceImage = _src.get(i);
                    facebitmap = Bitmap.createBitmap(faceImage.cols(), faceImage.rows(), Bitmap.Config.ARGB_8888);
                    Utils.matToBitmap(faceImage, facebitmap);
                    if (facebitmap != null) {
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

    public void read_cvs(String _string) {
        FileInputStream fis = null;
        InputStreamReader isr = null;
        BufferedReader br = null; //用于包装InputStreamReader,提高处理性能。因为BufferedReader有缓冲的，而InputStreamReader没有。
        try {
            String str = "";
            fis = new FileInputStream("/sdcard/myImage/Database/face/at.txt");// FileInputStream
            // 从文件系统中的某个文件中获取字节
            isr = new InputStreamReader(fis);// InputStreamReader 是字节流通向字符流的桥梁,
            br = new BufferedReader(isr);// 从字符输入流中读取文件中的内容,封装了一个new InputStreamReader的对象
            String line, path, name;
            int curLabel = 0, index;
            Mat temp_img = new Mat();
            while ((str = br.readLine()) != null) {
                index = str.indexOf(";");
                path = "/sdcard/myImage/" + str.substring(0, index);
                name = str.substring(index + 1);

                if (!path.isEmpty() && !name.isEmpty()) {
                    File imfile = new File(path);
                    if (imfile.exists()) {
                        _name.add(name);
                        _label_array.add(curLabel++);
                        temp_img = Imgcodecs.imread(path);
                        Mat gray_img = new Mat();
                        Imgproc.cvtColor(temp_img, gray_img, Imgproc.COLOR_BGR2GRAY);
                        _src.add(gray_img);
                    }
                }
            }
            Integer[] lbs = new Integer[_label_array.size()];
            byte[] test = new byte[_label_array.size()];
            _label_array.toArray(lbs);
            for (int is = 0; is < _label_array.size(); is++)
                test[is] = (byte) (lbs[is] & 0xff);
            _labels = new Mat(_label_array.size(), 1, CvType.CV_8UC1);
            _labels.put(0, 0, test);
        } catch (FileNotFoundException e) {
            System.out.println("找不到指定文件");
        } catch (IOException e) {
            System.out.println("读取文件失败");
        } finally {
            _database_status = true;
            try {
                br.close();
                isr.close();
                fis.close();
                // 关闭的时候最好按照先后顺序关闭最后开的先关闭所以先关s,再关n,最后关m
            } catch (IOException e) {
                e.printStackTrace();
            }

        }
    }

    public void write_cvs(Mat image, String filepath, String new_name) {
        FileOutputStream fis = null;
        OutputStreamWriter isw = null;
        BufferedWriter bw = null; //用于包装InputStreamReader,提高处理性能。因为BufferedReader有缓冲的，而InputStreamReader没有。
        try {
            String str = "";
            File fp = new File("/sdcard/myImage/Database/face/at.txt");
            if (fp.exists()) fp.delete();
            fis = new FileOutputStream("/sdcard/myImage/Database/face/at.txt");// FileOutputStream
            // 从文件系统中的某个文件中获取字节
            isw = new OutputStreamWriter(fis);// InputStreamReader 是字节流通向字符流的桥梁,
            bw = new BufferedWriter(isw);// 从字符输入流中读取文件中的内容,封装了一个new InputStreamReader的对象
            String line, path, name;
            int curLabel = 0, index;
            Mat temp_img = new Mat();
            for (int i = 0; i < _name.size(); i++) {
                String writeln = "./Database/face" + ".jpg;" + _name;
            }
            // while ((str = bw.write();Line()) != null)
            {
                index = str.indexOf(";");
                path = "/sdcard/myImage/" + str.substring(0, index);
                name = str.substring(index + 1);
                if (!path.isEmpty() && !name.isEmpty()) {
                    File imfile = new File(path);
                    if (imfile.exists()) {
                        _name.add(name);
                        _label_array.add(curLabel++);
                        temp_img = Imgcodecs.imread(path);
                        Mat gray_img = new Mat();
                        Imgproc.cvtColor(temp_img, gray_img, Imgproc.COLOR_BGR2GRAY);
                        _src.add(gray_img);
                    }
                }
            }
            Integer[] lbs = new Integer[_label_array.size()];
            byte[] test = new byte[_label_array.size()];
            _label_array.toArray(lbs);
            for (int is = 0; is < _label_array.size(); is++)
                test[is] = (byte) (lbs[is] & 0xff);
            _labels = new Mat(_label_array.size(), 1, CvType.CV_8UC1);
            _labels.put(0, 0, test);
        } catch (IOException e) {
            System.out.println("读取文件失败");
        } finally {
            _database_status = true;
            try {
                bw.close();
                isw.close();
                fis.close();
                // 关闭的时候最好按照先后顺序关闭最后开的先关闭所以先关s,再关n,最后关m
            } catch (IOException e) {
                e.printStackTrace();
            }

        }
    }

    public void train() {
        if (_src.size() == 0) {
            //string error_message = format("Empty training data was given. You'll need more than one sample to learn a model.");
            //CV_Error(CV_StsBadArg, error_message);
        } else if (_labels.type() != CvType.CV_32SC1) {
            //string error_message = format("Labels must be given as integer (CV_32SC1). Expected %d, but was %d.", CV_32SC1, _local_labels.type());
            //CV_Error(CV_StsBadArg, error_message);
        }
        // make sure data has correct size
        if (_src.size() > 1) {
            for (int i = 1; i < _src.size(); i++) {
                if (_src.get(i - 1).size() != _src.get(i).size()) {
                    //getMat(i-1).total() != _src.getMat(i).total()) {
                    //string error_message = format("In the Eigenfaces method all input samples (training images) must be of equal size! Expected %d pixels, but was %d pixels.", _src.getMat(i-1).total(), _src.getMat(i).total());
                    //CV_Error(CV_StsUnsupportedFormat, error_message);
                }
                /*Mat faceImage=new Mat();
                _src.get(i).copyTo(faceImage);
                Imgproc.cvtColor(faceImage, faceImage, Imgproc.COLOR_GRAY2BGR);
                Bitmap facebitmap = Bitmap.createBitmap(faceImage.cols(), faceImage.rows(), Bitmap.Config.ARGB_8888);
                Utils.matToBitmap(faceImage, facebitmap);*/
            }
        }
        // get labels
        // Mat labels = _labels;
        // observations in row
        Mat data = asRowMatrix(_src, CvType.CV_64FC1);

        // number of samples
        int n = data.rows();
        // assert there are as much samples as labels
        if (_labels.rows() != n) {
            // string error_message = format("The number of samples (src) must equal the number of labels (labels)! len(src)=%d, len(labels)=%d.", n, labels.total());
            //CV_Error(CV_StsBadArg, error_message);
        }
        // clear existing model data
        //if(_labels!=null) _labels.release();
        if (_projections != null) _projections.clear();
        // clip number of components to be valid
        if ((_num_components <= 0) || (_num_components > n))
            _num_components = n;

        // perform the PCA
        //PCA pca(data, Mat(), CV_PCA_DATA_AS_ROW, _num_components);
        _mean = new Mat();
        _eigenvectors = new Mat();
        _eigenvalues = new Mat();
        _projections = new LinkedList<Mat>();
        Mat mean = new Mat();
        Mat eigenvectors = new Mat();
        Core.PCACompute(data, mean, _eigenvectors, _num_components);
        //Core.transpose(eigenvectors,_eigenvectors);
        _mean = mean.reshape(1, 1);
        // copy the PCA results
        //_mean = pca.mean.reshape(1,1); // store the mean vector
        //_eigenvalues = pca.eigenvalues.clone(); // eigenvalues by row
        //transpose(pca.eigenvectors, _eigenvectors); // eigenvectors by column
        // store labels for prediction
        // _labels = labels.clone();
        // save projections
        for (int sampleIdx = 0; sampleIdx < data.rows(); sampleIdx++) {
            Mat p = new Mat();
            Core.PCAProject(data.row(sampleIdx), _mean, _eigenvectors, p);
            //Mat p = subspaceProject(_eigenvectors, _mean, data.row(sampleIdx));
            _projections.add(p);
        }
    }

    ;

    public Mat asRowMatrix(List<Mat> src, int rtype) {
        // 样本个数
        double alpha = 1, beta = 0;
        int n = src.size();
        // 如果样本为空，返回空矩阵
        if (n == 0)
            return new Mat();
        // 样本的维度
        long d = src.get(0).total();
        // 构建返回矩阵
        Mat data = new Mat(n, (int) d, rtype);
        //Mat data(n, d, rtype);
        // 将图像数据复制到结果矩阵中
        for (int i = 0; i < n; i++) {
            //如果数据为空，抛出异常
            if (src.get(i).empty()) {
                //string error_message = format("Image number %d was empty, please check your input data.", i);
                //CV_Error(CV_StsBadArg, error_message);
                return new Mat();
            }
            // 图像数据的维度要是d，保证可以复制到返回矩阵中
            if (src.get(i).total() != d) {
                //string error_message = format("Wrong number of elements in matrix #%d! Expected %d was %d.", i, d, src[i].total());
                //CV_Error(CV_StsBadArg, error_message);
                return new Mat();
            }
            // 获得返回矩阵中的当前行矩阵:
            Mat xi = new Mat();//data.row(i);
            // 将一副图像映射到返回矩阵的一行中:
            if (src.get(i).isContinuous()) {
                src.get(i).reshape(1, 1).convertTo(xi, rtype, alpha, beta);
            } else {
                src.get(i).clone().reshape(1, 1).convertTo(xi, rtype, alpha, beta);
            }
            xi.copyTo(data.row(i));
        }
        return data;
    }

    public Mat asRowMatrix(Mat src, int rtype) {
        // 样本个数
        double alpha = 1, beta = 0;
        // 样本的维度
        Size d = src.size();
        // 构建返回矩阵
        Mat data = new Mat(d, rtype);
        //Mat data(n, d, rtype);
        // 将图像数据复制到结果矩阵中
        //如果数据为空，抛出异常
        if (src.empty()) {
            //string error_message = format("Image number %d was empty, please check your input data.", i);
            //CV_Error(CV_StsBadArg, error_message);
            return new Mat();
        }
        // 图像数据的维度要是d，保证可以复制到返回矩阵中
        if (src.size().height != d.height || src.size().width != d.width) {
            //string error_message = format("Wrong number of elements in matrix #%d! Expected %d was %d.", i, d, src[i].total());
            //CV_Error(CV_StsBadArg, error_message);
            return new Mat();
        }
        // 获得返回矩阵中的当前行矩阵:
        Mat xi = new Mat();
        // 将一副图像映射到返回矩阵的一行中:
        if (src.isContinuous()) {
            src.reshape(1, 1).convertTo(xi, rtype, alpha, beta);
        } else {
            src.clone().reshape(1, 1).convertTo(xi, rtype, alpha, beta);
        }
        xi.copyTo(data);
        return data;
    }

    // Predicts the label and confidence for a given sample.

    public int predict(Mat src, int minClass, double minDist, List<Mat> _projections, Mat _eigenvectors, Mat _mean, Mat _labels, int _match_ratio) {
        //void Eigenfaces::predict(InputArray _src, int &minClass, double &minDist) const {
        // get data
        //Mat src = _src.getMat();
        // make sure the user is passing correct data
        if (_projections.isEmpty()) {
            // throw error if no data (or simply return -1?)
            //string error_message = "This Eigenfaces model is not computed yet. Did you call Eigenfaces::train?";
            //CV_Error(CV_StsError, error_message);
        } else if (_eigenvectors.rows() != src.rows()) {
            // check data alignment just for clearer exception messages
            //string error_message = format("Wrong input image size. Reason: Training and Test images must be of equal size! Expected an image with %d elements, but got %d.", _eigenvectors.rows, src.total());
            //CV_Error(CV_StsBadArg, error_message);
        }
        // project into PCA subspace
        // Mat q = subspaceProject(_eigenvectors, _mean, src.reshape(1,1));
        Mat q = new Mat();
        //Core.PCAProject(src,_mean,_eigenvectors,q);
        Mat data = asRowMatrix(src, CvType.CV_64FC1);
        Core.PCAProject(data, _mean, _eigenvectors, q);
        //minDist = 6000;//DBL_MAX;
        minClass = -1;
        double threshold = minDist;
        minDist = 1000000000;
        ;
        for (int sampleIdx = 0; sampleIdx < _projections.size(); sampleIdx++) {
            double dist = Core.norm(_projections.get(sampleIdx), q, Core.NORM_L2);
            if ((dist < minDist))// && (dist < _threshold))
            {
                minDist = dist;
                byte temp[] = new byte[1];
                _labels.get(sampleIdx, 0, temp);
                minClass = (int) temp[0];
            }
        }
        if (minDist < 1000) minDist = 1000;
        if (minClass > -1)
            _match_ratio = (int) ((Math.exp(-(minDist - 1000) / 4000)) * 100);//minDist;//((1-(minDist/threshold/4))*100);
        else _match_ratio = 0;

        return _match_ratio;
    }

    ;


    public int predict(Mat src, int minClass, double minDist) {
        //void Eigenfaces::predict(InputArray _src, int &minClass, double &minDist) const {
        // get data
        //Mat src = _src.getMat();
        // make sure the user is passing correct data
        if (_projections.isEmpty()) {
            // throw error if no data (or simply return -1?)
            //string error_message = "This Eigenfaces model is not computed yet. Did you call Eigenfaces::train?";
            //CV_Error(CV_StsError, error_message);
        } else if (_eigenvectors.rows() != src.rows()) {
            // check data alignment just for clearer exception messages
            //string error_message = format("Wrong input image size. Reason: Training and Test images must be of equal size! Expected an image with %d elements, but got %d.", _eigenvectors.rows, src.total());
            //CV_Error(CV_StsBadArg, error_message);
        }
        // project into PCA subspace
        // Mat q = subspaceProject(_eigenvectors, _mean, src.reshape(1,1));
        Mat q = new Mat();
        //Core.PCAProject(src,_mean,_eigenvectors,q);
        // 转换为64位
        Mat data = asRowMatrix(src, CvType.CV_64FC1);
        // PCA降维
        Core.PCAProject(data, _mean, _eigenvectors, q);
        //minDist = 6000;//DBL_MAX;
        minClass = -1;
        double threshold = minDist;
        minDist = 1000000000;

        for (int sampleIdx = 0; sampleIdx < _projections.size(); sampleIdx++) {
            double dist = Core.norm(_projections.get(sampleIdx), q, Core.NORM_L2);
            if ((dist < minDist))// && (dist < _threshold))
            {
                minDist = dist;
                byte temp[] = new byte[1];
                _labels.get(sampleIdx, 0, temp);
                minClass = (int) temp[0];
            }
        }
        if (minDist < 1000) {
            minDist = 1000;
        }
        if (minClass > -1) {
            _match_ratio = (int) ((Math.exp(-(minDist - 1000) / 4000)) * 100);//minDist;//((1-(minDist/threshold/4))*100);
        } else {
            _match_ratio = 0;
        }

        q.release();
        data.release();
        return minClass;
    }

    public boolean write_dbase() {
        dataBase.read_dbase_from_image();
        return dataBase.write_dbase();

    }

    public boolean read_dbase() {
        clear();
        boolean result = dataBase.read_dbase();
        if (result) {
            _labels = dataBase._labels;
            _src = dataBase._src;
            _name = dataBase._name;
            _people = dataBase._people;
            _label_array = dataBase._label_array;
            _date = dataBase._date;
            _nth = dataBase._nth;
            _rec_ratio = dataBase._rec_ratio;
            _database_status = true;
            _people_num = dataBase._people_num;
            return true;
        } else
            return false;


    }

    // See FaceRecognizer::load.
    /*public void load( FileStorage fs) {
    };

    // See FaceRecognizer::save.
    public void save(FileStorage fs) {
    };*/
}




