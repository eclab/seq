package seq.gui;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;

public class MainPane extends JPanel implements MouseListener {


    JPanel modulePane;
    public MainPane() {


        //this.setSize(new Dimension(700,500));
        this.setBackground(Color.gray);
        this.setLayout(new BorderLayout());

        Library libPane = new Library(this);



        JPanel inspectorPane = new JPanel();
        inspectorPane.setBackground(Color.lightGray);
        inspectorPane.setPreferredSize(new Dimension(200,500));


        modulePane = new JPanel();
        modulePane.setPreferredSize(new Dimension(500,500));
        modulePane.setBackground(Color.WHITE);



        JLabel prova = new JLabel("Module View");
        modulePane.add(prova);

        JLabel i = new JLabel("Inspector");
        inspectorPane.add(i);

        this.add(libPane,BorderLayout.WEST);
        this.add(modulePane,BorderLayout.CENTER);
        this.add(inspectorPane,BorderLayout.EAST);


        }

    @Override
    public void mouseClicked(MouseEvent e) {
        if(e.getSource() instanceof javax.swing.JLabel){
            JLabel diffLabelBecause = new JLabel();
            diffLabelBecause.setText(((JLabel) e.getSource()).getText());
            modulePane.removeAll();
            modulePane.add(diffLabelBecause);
            modulePane.revalidate();
            modulePane.repaint();
            }
        }

    @Override
    public void mousePressed(MouseEvent e) {

        }

    @Override
    public void mouseReleased(MouseEvent e) {

        }

    @Override
    public void mouseEntered(MouseEvent e) {

        }

    @Override
    public void mouseExited(MouseEvent e) {

        }
    }
