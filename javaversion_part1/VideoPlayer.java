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

public class VideoPlayer extends JPanel implements ActionListener {


    private static final long serialVersionUID = -8426080774234368297L;

    public static final int WIDTH = 480;
    public static final int HEIGHT = 270;

    Timer timer;

    BufferedImage img;
    InputStream videoStream;
    JLabel frame;

    JButton play, pause, stop;

    File audio;
    AudioInputStream audioStream;
    AudioFormat format;
    DataLine.Info info;
    Clip clip;
    JPanel pane;
    String videopath;
    String audiopath;

    boolean debug = false;
    GridBagConstraints layout;
    int curFrame;
    JFrame f;
    public static final int FRAMERATE = 30;

    public VideoPlayer(String videoPath, String audioPath) {
        int period = 1000 / FRAMERATE;

        videopath = videoPath;
        audiopath = audioPath;

        timer = new Timer(period/2, this);

        img = new BufferedImage(WIDTH, HEIGHT, BufferedImage.TYPE_INT_RGB);
        try {
            videoStream = new FileInputStream(videopath);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }

        frame = new JLabel(new ImageIcon(img));
        play = new JButton("Play");
        pause = new JButton("Pause");
        stop = new JButton("Stop");

        f = new JFrame("Java Video Player");
        f.setVisible(true);
        f.setSize(600, 400);
        f.setLocation(300,200);
        f.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        pane = new JPanel(new GridBagLayout());
        layout = new GridBagConstraints();
        pane.setLayout(new GridBagLayout());

        layout.gridx = 0; layout.gridy = 0;
        layout.gridwidth = 3;
        pane.add(frame, layout);
        pane.add(play, layout);

        layout.fill = GridBagConstraints.HORIZONTAL;
        layout.gridx = 0;
        layout.gridy = 1;
        layout.gridwidth = 1;
        layout.weightx = 1;
        pane.add(play, layout);

        layout.gridx = 1;
        pane.add(pause, layout);

        layout.gridx = 2;
        pane.add(stop, layout);

        play.addActionListener(this);
        pause.addActionListener(this);
        stop.addActionListener(this);

        f.add(pane);
        audio = new File(audiopath);
        try {
            audioStream = AudioSystem.getAudioInputStream(audio);
            format = audioStream.getFormat();
            info = new DataLine.Info(Clip.class, format);
            clip = (Clip)AudioSystem.getLine(info);
            clip.open(audioStream);
            clip.start();
        } catch (Exception e) {
            e.printStackTrace();
        }

        timer.start();
    }

    public void actionPerformed(ActionEvent arg0) {
        if (arg0.getSource() == pause) {
            if (clip.isActive()) {
                clip.stop();
            }
        }
        else if (arg0.getSource() == play) {
            if (!clip.isActive()) {
                clip.start();
            }
        }
        else if (arg0.getSource() == stop) {
            clip.stop();
            clip.close();

            try {
                audioStream = AudioSystem.getAudioInputStream(audio);
                format = audioStream.getFormat();
                info = new DataLine.Info(Clip.class, format);
                clip = (Clip)AudioSystem.getLine(info);
                clip.open(audioStream);

                curFrame = 0;
                videoStream.close();
                videoStream = new FileInputStream(videopath);

                for(int y = 0; y < HEIGHT; y++){
                    for(int x = 0; x < WIDTH; x++){
                        img.setRGB(x,y,0);
                    }
                }
                f.repaint();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        else {
            if (!clip.isActive()) {
                return;
            }
            try {
                byte[] bytes = new byte[WIDTH*HEIGHT*3];

                int audioFrame = (int)(clip.getFramePosition() / format.getFrameRate() * FRAMERATE);

                if (audioFrame < curFrame) {
                    return;
                }
                while (audioFrame > curFrame) {
                    int offset = 0;
                    int numRead = 0;
                    while (offset < bytes.length && (numRead=videoStream.read(bytes, offset, bytes.length-offset)) >= 0) {
                        offset += numRead;
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
                            // frame = new JLabel(new ImageIcon(img));
                            // pane.add(frame, layout);


                            ++ind;
                        }
                    }
                    ++curFrame;
                }
                f.repaint();
            } catch (IOException e) {
                e.printStackTrace();
            }

        }
    }
}
