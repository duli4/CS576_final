import java.lang.String;
import java.util.List;
import java.util.Map;
import java.util.Stack;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import com.github.psambit9791.wavfile.WavFile;
import com.github.psambit9791.wavfile.WavFileException;
import java.io.*;
import org.opencv.core.*;
import org.opencv.imgproc.Imgproc;


class logoDetect {
	static InputStream starStream;
	static InputStream tmpStream;
	static InputStream new1Stream;
	static InputStream new2Stream;
	static InputStream audioStream;
	static InputStream videoStream;
	static FileOutputStream fout;
	static FileOutputStream vout;
	static WavFile win;
	static WavFile wnew1;
	static WavFile wnew2;
	static WavFile wout;
	static Map<String, List<Integer>> map;
    public static void main(String[] args) throws IOException {
        System.loadLibrary(Core.NATIVE_LIBRARY_NAME);
        AdvHandler advHandler = new AdvHandler(args[0], args[1]);
        Stack<Integer> result = advHandler.get_frame_list();
        System.out.println("result: " + result);
        detect(args[0]);
        System.out.println("map: " + map);
        deleteReplace(args[1], args[2], args[3], result);
        
        
    }
    
    public static int findmin(List<Integer> l, int value) {
    	int result = 10000;
    	for(int i = 0; i < l.size(); i++) {
    		int diff = value - l.get(i);
    		if ((diff < result) && diff >= 0) {
    			result = diff;
    		}
    	}
    	return result;
    }
    // mode 0 is normal frame
    // mode 1 is for brand1 - nfl
    // mode 2 is for brand2 - mcd
    public static int getmode(Stack<Integer> adv, int count) {
    	for(int i = 0; i < adv.size(); i = i + 2) {
        	if (i == 0) {
        		if (count >= 0 && count < adv.get(0)) {
        			return 0;
        		}
        	}
        	if (i + 2 < adv.size())  {
        		if (count >= adv.get(i + 1) && count < adv.get(i + 2)) {
        			return 0;
        		}
        	}
        	if (i + 2 >= adv.size()) {
        		if (count >= adv.get(i + 1) && count < 9000) {
        			return 0;
        		}
        	}
        	if (count >= adv.get(i) && count < adv.get(i + 1)) {
        		int diff1 = findmin(map.get("nfl"), adv.get(i));
        		int diff2 = findmin(map.get("mcd"), adv.get(i));
        		if (diff1 == 10000 && diff2 == 10000) {
        			return 0;
        		}
        		if (diff1 < diff2) {
        			return 1;
        		} else {
        			return 2;
        		}
        	}
        }
    	System.out.println("error:" + count);
    	return -1;
    }
    
    public static void deleteReplace(String inputwav, String outputrgb, String outputwav, Stack<Integer> adv) throws IOException {
    	String path = "./tmp.rgb";
    	// open readin video file
    	try {
            tmpStream = new FileInputStream(path);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
    	// open write to video file
    	try {
        	vout = new FileOutputStream(outputrgb);
        } catch (Exception e) {
        	e.printStackTrace();
        }
    	
    	try {
        	win = WavFile.openWavFile(new File(inputwav));
        	wout = WavFile.newWavFile(new File(outputwav), 1, 300*48000, 16, 48000);
        } catch (Exception e) {
        	e.printStackTrace();
        }
    	int[] buffer = new int[1600];
    	int count = 0;
    	int index = 0;
    	int mode = 0;
    	int read = 0;
    	boolean done1 = true;
    	boolean done2 = true;
    	while(true) {
    		try {
    			read = win.readFrames(buffer, 1600);
    		} catch (Exception e) {
    			e.printStackTrace();
    		}
    		byte[] byteslast = new byte[480*270*3];
            int offsetlast = 0;
            int numReadlast = 0;
            while (offsetlast < byteslast.length && (numReadlast=tmpStream.read(byteslast, offsetlast, byteslast.length-offsetlast)) >= 0) {
                offsetlast += numReadlast;
            }
            if (numReadlast <= 0 || read <= 0) {
            	win.close();
                break;
            }
            mode = getmode(adv, count);
            if (mode == -1) {
            	System.out.println("ERROR!!! return -1");
            	System.out.println(numReadlast);
            	break;
            }
            if (mode == 0) {
            	vout.write(byteslast);
            	try {
					wout.writeFrames(buffer, 1600);
				} catch (Exception e) {
					e.printStackTrace();
				} 
            	
            }
            if (mode == 1 && done1) {
            	System.out.println("current frame: " + count);
            	try {
                    new1Stream = new FileInputStream("./dataset2/Ads/nfl_Ad_15s.rgb");
                } catch (FileNotFoundException e) {
                    e.printStackTrace();
                }
            	while(true) {
            		byte[] bytes1 = new byte[480*270*3];
                    int offset1 = 0;
                    int numRead1 = 0;
            		while (offset1 < bytes1.length && (numRead1=new1Stream.read(bytes1, offset1, bytes1.length-offset1)) >= 0) {
                        offset1 += numRead1;
                    }
            		if (numRead1 < 0) {
                        break;
                    }
            		vout.write(bytes1);
            	}
            	done1 = false;
            	
            	try {
                	wnew1 = WavFile.openWavFile(new File("./dataset2/Ads/nfl_Ad_15s.wav"));
                	int frameread = 0;
                	do {
                		frameread = wnew1.readFrames(buffer, 1600);
                		wout.writeFrames(buffer, frameread);
                	}
                	while (frameread != 0);
                } catch (Exception e) {
                	e.printStackTrace();
                }
            }
            if (mode == 2 && done2) {
            	System.out.println("current frame: " + count);
            	try {
                    new2Stream = new FileInputStream("./dataset2/Ads/mcd_Ad_15s.rgb");
                } catch (FileNotFoundException e) {
                    e.printStackTrace();
                }
            	while(true) {
            		byte[] bytes2 = new byte[480*270*3];
                    int offset2 = 0;
                    int numRead2 = 0;
            		while (offset2 < bytes2.length && (numRead2=new2Stream.read(bytes2, offset2, bytes2.length-offset2)) >= 0) {
                        offset2 += numRead2;
                    }
            		if (numRead2 < 0) {
                        break;
                    }
            		vout.write(bytes2);
            	}
            	done2 = false;
            	try {
                	wnew2 = WavFile.openWavFile(new File("./dataset2/Ads/mcd_Ad_15s.wav"));
                	int frameread1 = 0;
                	do {
                		frameread1 = wnew2.readFrames(buffer, 1600);
                		wout.writeFrames(buffer, frameread1);
                	}
                	while (frameread1 != 0);
                } catch (Exception e) {
                	e.printStackTrace();
                }
            }
            count++;
            
    	}
    	
    	
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
            starStream = new FileInputStream("./dataset2/Brand Images/" + brand);
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
        Mat h_brand1 = getBrandhist("Mcdonalds_logo.raw");
        Mat h_brand2 = getBrandhist("nfl_logo.rgb");
        map  = new HashMap<String, List<Integer>>();
        List<Integer> ph1 = new ArrayList<Integer>();
        List<Integer> ph2 = new ArrayList<Integer>();
        
        
        // read one frame from video Path
        try {
            videoStream = new FileInputStream(videoPath);  
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        int windoww = 128;
        int windowh = 120;
        int shiftw = 480 - windoww;
        int shifth = 270 - windowh;
        int count = 0;
        int total = 270 * 480;
        boolean change = false;
        boolean change1 = false;
        
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
            
            if (((count >= 2220 && count <= 2250) || (count >=4260 && count <= 4440)) && count % 10 == 0) {
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
		        if (/*max_compare2 > 0.1 && max_compare2 < 0.15 &&*/ (count >= 2220 && count <= 2250)) {
		        	change = true;
		        	ph1.add(count);
		        } else {
		        	change = false;
		        }
		        if (/*max_compare1 > 0.09 && max_compare1 < 0.104 &&*/ (count >= 4260 && count <= 4440)) {
		        	change1 = true;
		        	ph2.add(count);
		        } else {
		        	change1 = false;
		        }
		        System.out.println("current frame: " + count);
		        System.out.println("correlated for 1: " + max_compare1);
		        System.out.println("correlated for 2: " + max_compare2);
		        System.out.println("x,y for 1: " + max_h1 + " " + max_w1);
		        System.out.println("correlated for 2: " + max_h2 + " " + max_w2);
		       
            }
            if ((count >= 2220 && count <= 2260)) {
            	if (change) {
    	        	for (int q = 0; q < 1; q++) {
    		        	for (int u = 0; u < windoww + 1; u++) {
    		        		bytes[u + max_w2 + (max_h2 + q) * 480] = (byte)255;
    		        		bytes[u + max_w2 + (max_h2 + q) * 480 + total] = (byte)255;
    		        		bytes[u + max_w2 + (max_h2 + q) * 480 + total * 2] = (byte)0;
    		        		bytes[u + max_w2 + (max_h2 + q + windowh) * 480] = (byte)255;
    		        		bytes[u + max_w2 + (max_h2 + q + windowh) * 480 + total] = (byte)255;
    		        		bytes[u + max_w2 + (max_h2 + q + windowh) * 480 + total * 2] = (byte)0;
    		        	}
    		        	for (int t = 0; t < windowh + 1; t++) {
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
            if ((count >= 4260 && count <= 4450)) {
            	if (change1) {
    	        	for (int q = 0; q < 2; q++) {
    		        	for (int u = 0; u < windoww; u++) {
    		        		bytes[u + max_w1 + (max_h1 + q) * 480] = (byte)255;
    		        		bytes[u + max_w1 + (max_h1 + q) * 480 + total] = (byte)0;
    		        		bytes[u + max_w1 + (max_h1 + q) * 480 + total * 2] = (byte)0;
    		        		bytes[u + max_w1 + (max_h1 + q + windowh) * 480] = (byte)255;
    		        		bytes[u + max_w1 + (max_h1 + q + windowh) * 480 + total] = (byte)0;
    		        		bytes[u + max_w1 + (max_h1 + q + windowh) * 480 + total * 2] = (byte)0;
    		        	}
    		        	for (int t = 0; t < windowh; t++) {
    		        		bytes[q + max_w1 + (max_h1 + t) * 480] = (byte)255;
    		        		bytes[q + max_w1 + (max_h1 + t) * 480 + total] = (byte)0;
    		        		bytes[q + max_w1 + (max_h1 + t) * 480 + total * 2] = (byte)0;
    		        		bytes[q + max_w1 + windoww + (max_h1 + t) * 480] = (byte)255;
    		        		bytes[q + max_w1 + windoww + (max_h1 + t) * 480 + total] = (byte)0;
    		        		bytes[q + max_w1 + windoww + (max_h1 + t) * 480 + total * 2] = (byte)0;
    		        	}
    		        }
    	        }
            }
            
            
            fout.write(bytes);
            if (numRead < 0) {
                break;
            }
            count++;
        }
        
        map.put("mcd", ph2);
        map.put("nfl", ph1);
    }
}
