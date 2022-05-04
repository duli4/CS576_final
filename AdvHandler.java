import java.awt.Color;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.lang.Math;
import java.util.*;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Clip;
import javax.sound.sampled.DataLine;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.Timer;


public class AdvHandler
{
  public static final int WIDTH = 480;
  public static final int HEIGHT = 270;
  String videopath;
  String audiopath;
  // Timer timer;

  BufferedImage prev_img;
  BufferedImage img3;
  int frame_count = 0;
  int sum_diff = 0;
  int index_de = 0;
  int diff_last_cut = 0;
  int R_total = 0, G_total =0 ,  B_toatl=0;
  int diff3R = 0, diff3G = 0, diff3B = 0;
  int temp_diff3;
  boolean debug = false;
  int difR = 0, difG = 0, difB = 0;

  int curFrame;
  Clip clip;
  File audio;
  AudioInputStream audioStream;
  AudioFormat format;
  DataLine.Info info;
  BufferedImage img;
  InputStream videoStream;

  Stack<Integer> time_clip = new Stack<Integer>();

  public static final int FRAMERATE = 30;

  public AdvHandler(String videoPath, String audioPath)
  {
    int period = 1000 / FRAMERATE;
    videopath = videoPath;
    audiopath = audioPath;
    // timer = new Timer(period/2, this);

    prev_img = new BufferedImage(WIDTH, HEIGHT, BufferedImage.TYPE_INT_RGB);
    for(int y = 0; y < HEIGHT; y++){
        for(int x = 0; x < WIDTH; x++){
            prev_img.setRGB(x,y,0);
        }
    }

    img3 = new BufferedImage(WIDTH, HEIGHT, BufferedImage.TYPE_INT_RGB);
    for(int y = 0; y < HEIGHT; y++){
        for(int x = 0; x < WIDTH; x++){
            img3.setRGB(x,y,0);
        }
    }


    img = new BufferedImage(WIDTH, HEIGHT, BufferedImage.TYPE_INT_RGB);
    try {
        videoStream = new FileInputStream(videopath);
    } catch (FileNotFoundException e) {
        e.printStackTrace();
    }


    while(true)
    {
      try
      {
      int offset = 0;
      int numRead = 0;
      byte[] bytes = new byte[WIDTH*HEIGHT*3];
      while (offset < bytes.length && (numRead=videoStream.read(bytes, offset, bytes.length-offset)) >= 0) {
          offset += numRead;
      }
      if(numRead < 0)
      {
        break;
      }
      int ind = 0;
      for(int y = 0; y < HEIGHT; y++){
          for(int x = 0; x < WIDTH; x++){
              int r = bytes[ind] & 0xff;
              int g = bytes[ind+HEIGHT*WIDTH] & 0xff;
              int b = bytes[ind+HEIGHT*WIDTH*2] & 0xff;

              if (debug) {
                  if (x < WIDTH / 4 || x > 3 * WIDTH / 4) {
                      g = 0;
                      b = 0;
                      r = 0;
                  }

                  float hsv[] = new float[3];
                  Color.RGBtoHSB(r, g, b, hsv);
                  if (hsv[1] > 0.8 || hsv[2] > 0.8) {
                      float h = hsv[0] * 240;
                      if (h > 70 && h < 120 && hsv[1] > .7) {
                          g = 255;
                          r = 0;
                          b = 70;
                      }
                      else if (h > 30 && h < 50) {
                          r = 255;
                          g = 255;
                          b = 0;
                      }
                      else if (h >= 120 && h < 165 && hsv[1] > .7) {
                          r = 0;
                          g = 30;
                          b = 200;
                      }
                      else if ((h > 220 || h < 10) && hsv[1] > .7) {
                          r = 255;
                          g = 0;
                          b = 20;
                      }
                      else if (hsv[1] < .1) {
                          r = 255;
                          g = 255;
                          b = 255;
                      }
                      else {
                          r = 0;
                          g = 0;
                          b = 0;
                      }
                  }
                  else if (g > 200 && b > 200 && r > 200) {
                      r = 255;
                      g = 255;
                      b = 255;
                  }
                  else {
                      g = 0;
                      b = 0;
                      r = 0;
                  }
              }
              int pix = 0xff000000 | ((r & 0xff) << 16) | ((g & 0xff) << 8) | (b & 0xff);
              // System.out.println("pix = " + pix);
              // BufferedImage img = new BufferedImage(WIDTH, HEIGHT, BufferedImage.TYPE_INT_RGB);
              img.setRGB(x,y,pix);
                Color c = new Color(prev_img.getRGB(x, y));
                difR += Math.abs(r - c.getRed());
                difG += Math.abs(g - c.getGreen());
                difB += Math.abs(b - c.getBlue());
                sum_diff = (difR + difG +difB);
                prev_img.setRGB(x, y, pix);

              ++ind;
          }
      }

      if (sum_diff/(HEIGHT * WIDTH) > 170)
      {
        // System.out.println("current time: " + curFrame/30 + "  differ sum = " + sum_diff/(HEIGHT * WIDTH));

        time_clip.push(curFrame/30);
            System.out.println("time list: " + time_clip);
        for(int y = 0; y < HEIGHT; y++){
            for(int x = 0; x < WIDTH; x++){
              Color c = new Color(prev_img.getRGB(x, y));
              int r = c.getRed();
              int g = c.getGreen();
              int b = c.getBlue();
              int pix = 0xff000000 | ((r & 0xff) << 16) | ((g & 0xff) << 8) | (b & 0xff);
              // System.out.println("current time: " + curFrame/30 + "  differ sum = " + sum_diff/(HEIGHT * WIDTH));
              img3.setRGB(x, y, pix);
            }
          }
      }
      difR = 0;
      difG = 0;
      difB = 0;

      sum_diff = 0;
      curFrame++;
    }catch (IOException e) {
        e.printStackTrace();
    }
  }

  }
  //
  public Stack<Integer> get_frame_list()
  {
    int flag = 0;
    Stack<Integer> res = new Stack<Integer>();
    Integer[] arr = null;
    arr = time_clip.toArray(new Integer[time_clip.size()]);
    int prev = arr[0];
    for(int i =1;i<time_clip.size();i++)
    {
      // System.out.println("nonono:" + arr[i]);
      if (arr[i] - prev <  13 && flag == 0)
      {
        res.push(prev * 30);
        flag = 1;
      }
      if(flag == 1 && arr[i] - prev >=13)
      {
        res.push(prev * 30);
        flag = 0;
      }
      prev = arr[i];
    }
    if(res.size() % 2 == 1)
    {
      res.push(arr[time_clip.size()-1] * 30);
    }


    Stack<Integer> ret = new Stack<Integer>();
    Integer[] arr1 = null;
    System.out.println("array : " + res);
    arr1 = res.toArray(new Integer[res.size()]);
    for(int i =0;i<res.size();i += 2)
    {
      if (arr1[i+1] - arr1[i] < 450)
      {
        continue;
      }
      else if(arr1[i+1] - arr1[i] > 450)
      {
        ret.push(arr1[i]);
        ret.push(arr1[i] + 450);
      }
      else{//when euqal
        ret.push(arr1[i]);
        ret.push(arr1[i+1]);
      }
    }
    return ret;
  }


  public static void main(String[] args)
  {
      AdvHandler advHandler = new AdvHandler(args[0], args[1]);
       Stack<Integer> result = advHandler.get_frame_list();
        System.out.println("arr:" + result);
  }
}
