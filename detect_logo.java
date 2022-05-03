import java.lang.String;
import java.util.List;

import javax.swing.ImageIcon;
import javax.swing.JLabel;

import java.util.ArrayList;
import java.util.Arrays;
import java.awt.image.BufferedImage;
import java.io.*;
import org.opencv.core.*;
import org.opencv.imgproc.Imgproc;


class logoDetect {
	static InputStream starStream;
	static InputStream videoStream;
	static FileOutputStream fout;
    public static void main(String[] args) throws IOException {
        System.loadLibrary(Core.NATIVE_LIBRARY_NAME);
        
        detect("./dataset/Videos/data_test1.rgb");
    }

    public static Mat getHist(Mat m, int width, int height) {
        Mat convertHSV = new Mat(height, width, CvType.CV_8UC3);
        // convert to HSV
        Imgproc.cvtColor(m, convertHSV, Imgproc.COLOR_RGB2HSV);
        List<Mat> hsvplane = Arrays.asList(convertHSV);
        
        // Histogram calculation only consider H
        float[] range = {0, 360, 0, 256};
        int channels[] = {0, 1}; 
        Mat hist = new Mat();
        int histSize[] = {50, 60};
        Imgproc.calcHist(hsvplane, new MatOfInt(channels), new Mat(), hist, new MatOfInt(histSize), new MatOfFloat(range), false);
        Core.normalize(hist, hist, 0, 1, Core.NORM_MINMAX);
        return hist;
    }

    public static double compare(Mat h1, Mat h2) {
    	double a = Imgproc.compareHist(h1, h2, Imgproc.CV_COMP_CORREL);
    	return a;
    }
    
    public static Mat getBrandhist(String brand) throws IOException {
    	// read from brand file
        try {
            starStream = new FileInputStream("./dataset/Brand Images/" + brand);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        byte[] bytes_star = new byte[480*270*3];
        int offset_star = 0;
        int numRead_star = 0;
        while (offset_star < bytes_star.length && (numRead_star=starStream.read(bytes_star, offset_star, bytes_star.length-offset_star)) >= 0) {
            offset_star += numRead_star;
        }
        Mat m = new Mat(270, 480, CvType.CV_8UC3);
        int ind = 0;
        for(int y = 0; y < 270; y++){
            for(int x = 0; x < 480; x++){
                byte r = bytes_star[ind];
                byte g = bytes_star[ind+270*480];
                byte b = bytes_star[ind+270*480*2];
                byte[] values = new byte[] {r, g, b};
                m.put(y, x, values);
                ind++;
            }
        }
        return getHist(m, 480, 270);
    }

    public static void detect(String videoPath) throws IOException {
        Mat h_brand1 = getBrandhist("starbucks_logo.rgb");
        Mat h_brand2 = getBrandhist("subway_logo.rgb");
        
        // read one frame from video Path
        try {
            videoStream = new FileInputStream(videoPath);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        int windoww = 72;
        int windowh = 40;
        int shiftw = 480 - windoww;
        int shifth = 270 - windowh;
        int count = 0;
        int total = 270 * 480;
        boolean change = false;
        try {
        	fout = new FileOutputStream("./tmp.rgb");
        } catch (Exception e) {
        	e.printStackTrace();
        }
        int max_w1 = 0;
        int max_h1 = 0;
        int max_w2 = 0;
        int max_h2 = 0;
        
        while (true) {
        	double max_compare1 = 0;
            double max_compare2 = 0;
            byte[] bytes = new byte[480*270*3];
            int offset = 0;
            int numRead = 0;
            
            while (offset < bytes.length && (numRead=videoStream.read(bytes, offset, bytes.length-offset)) >= 0) {
                offset += numRead;
            }
            if (numRead < 0) {
                break;
            }
            if ((count >= 1800 && count <= 2020) && count % 10 == 0) {
		        Mat mf = new Mat(windowh, windoww, CvType.CV_8UC3);
		        for (int i = 0; i < shiftw; i = i + 5) {
		        	for (int j = 0; j < shifth; j = j + 5) {
		        		for (int k = 0; k < windoww; k++) {
		        			for (int c = 0; c < windowh; c++) {
		        				int cal = k + i + (c + j) * 480;
		        				byte r = bytes[cal];
		        				byte g = bytes[cal + total];
		                        byte b = bytes[cal + total * 2];
		                        byte[] values = new byte[] {r, g, b};
		                        mf.put(c, k, values);
		        			}
		        		}
		        		Mat h_win = getHist(mf, windoww, windowh);
		        		double compare1 = compare(h_win, h_brand1);
		        		double compare2 = compare(h_win, h_brand2);
		        		if (compare1 > max_compare1) {
		        			max_compare1 = compare1;
		        			max_h1 = j;
		        			max_w1 = i;
		        		}
		        		if (compare2 > max_compare2) {
		        			max_compare2 = compare2;
		        			max_h2 = j;
		        			max_w2 = i;
		        		}
		        	}
		        }
		        if (max_compare2 > 0.1 && max_compare2 < 0.1114) {
		        	change = true;
		        } else {
		        	change = false;
		        }
		        System.out.println("current frame: " + count);
		        //System.out.println("correlated for 1: " + max_compare1);
		        System.out.println("correlated for 2: " + max_compare2);
		        //System.out.println("x,y for 1: " + max_h1 + " " + max_w1);
		        //System.out.println("correlated for 2: " + max_h2 + " " + max_w2);
		        
            }
            if (count >= 1800 && count <= 2020) {
	            if (change) {
		        	for (int q = -1; q < 1; q++) {
			        	for (int u = -1; u < windoww + 1; u++) {
			        		bytes[u + max_w2 + (max_h2 + q) * 480] = (byte)255;
			        		bytes[u + max_w2 + (max_h2 + q) * 480 + total] = (byte)255;
			        		bytes[u + max_w2 + (max_h2 + q) * 480 + total * 2] = (byte)0;
			        		bytes[u + max_w2 + (max_h2 + q + windowh) * 480] = (byte)255;
			        		bytes[u + max_w2 + (max_h2 + q + windowh) * 480 + total] = (byte)255;
			        		bytes[u + max_w2 + (max_h2 + q + windowh) * 480 + total * 2] = (byte)0;
			        	}
			        	for (int t = -1; t < windowh + 1; t++) {
			        		bytes[q + max_w2 + (max_h2 + t) * 480] = (byte)255;
			        		bytes[q + max_w2 + (max_h2 + t) * 480 + total] = (byte)255;
			        		bytes[q + max_w2 + (max_h2 + t) * 480 + total * 2] = (byte)0;
			        		bytes[q + max_w2 + windoww + (max_h2 + t) * 480] = (byte)255;
			        		bytes[q + max_w2 + windoww + (max_h2 + t) * 480 + total] = (byte)255;
			        		bytes[q + max_w2 + windoww + (max_h2 + t) * 480 + total * 2] = (byte)0;
			        	}
			        }
		        }
            }
            fout.write(bytes);
            count++;
        }
        
    }
}
