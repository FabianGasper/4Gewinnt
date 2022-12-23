package org.fg.VierGewinnt.VierGFX;

import javafx.animation.Animation;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.paint.Color;
import javafx.scene.paint.Paint;
import javafx.scene.shape.Circle;
import javafx.util.Duration;
import org.fg.VierGewinnt.VierGEngine.AI;
import org.fg.VierGewinnt.VierGEngine.AIBitAlgebra;
import org.fg.VierGewinnt.VierGEngine.AISettings;
import org.fg.VierGewinnt.VierGEngine.AISettingsParameter;
import org.fg.VierGewinnt.VierGModel.Board;


public class GuiController {

    public Circle in1_b;
    public Circle in2_b;
    public Circle in3_b;
    public Circle in4_b;
    public Circle in5_b;
    public Circle in6_b;
    public Circle in7_b;
    public CheckBox ai_cb1;
    public CheckBox ai_cb2;
    public TextField ki_tl1;
    public TextField ki_tl2;
    public TextField ki_nt1;
    public TextField ki_nt2;
    public Button start_b;
    public Button reset_b;
    public ProgressBar progress_pb;
    public TextArea info_ta;
    public HBox inserts_hb;
    public HBox inserts_hbt;
    public GridPane grid;
    public TextField ki_md1;
    public TextField ki_md2;
    public Button stop_b;
    private AI ai1;
    private AI ai2;
    private final Node[][] gridNodes = new Node[7][6];
    private Task<Integer> task = null;

    private Circle[] buttons;
    private Board board = new Board();

    @FXML
    public void initialize() {
        Platform.runLater(() -> start_b.requestFocus());
    }

    private void button_insert(int i) {
        log((board.getTurn() ? "Spieler 1 (rot)" : "Spieler 2 (gelb)") + " hat gespielt: " + (i + 1));
        board.add(i);
        inserts_hb.setVisible(false);
        inserts_hbt.setVisible(false);
        start_b.requestFocus();
        advance();
    }

    public void in1() {
        button_insert(0);
    }

    public void in2() {
        button_insert(1);
    }

    public void in3() {
        button_insert(2);
    }

    public void in4() {
        button_insert(3);
    }

    public void in5() {
        button_insert(4);
    }

    public void in6() {
        button_insert(5);
    }

    public void in7() {
        button_insert(6);
    }

    public void start() {
        initGui();
        if (start_b.getText().equals("Start"))
            info_ta.setText("> Spiel gestartet...");
        else {
            log(">>> Spiel wird fortgesetzt...");
        }
        start_b.setDisable(true);
        reset_b.setDisable(false);
        stop_b.setDisable(false);
        if (ai_cb1.isSelected()) {
            ai1 = new AI(getSettings1(), board);
        }
        if (ai_cb2.isSelected()) {
            ai2 = new AI(getSettings2(), board);
        }
        advance();
    }

    private void initGui() {
        buttons = new Circle[]{in1_b, in2_b, in3_b, in4_b, in5_b, in6_b, in7_b};

        if (grid.getChildren().size() == 0) {
            for (int i = 0; i < 7; i++) {
                for (int j = 0; j < 6; j++) {
                    GridPane p = new GridPane();
                    grid.add(p, i, j);
                    gridNodes[i][j] = p;
                }
            }
        }
    }

    private void advance() {

        log("Brett-Bewertung aus Sicht von Rot: " + AIBitAlgebra.eval(board.getRed(), board.getYellow(), false));
        render();

        if (board.isWon()) {
            log("Fertig: " + (!board.getTurn() ? "Spieler 1 (rot)" : "Spieler 2 (gelb)") + " hat gewonnen");
            render();
            return;
        }
        if (board.isDraw()) {
            log("Fertig: unentschieden");
            render();
            return;
        }

        if (board.getTurn()) {
            log("Spieler 1 (rot) ist am Zug...");
            if (ai_cb1.isSelected()) {
                kiAdvance(ai1);
            } else {
                playerAdvance();
            }
        } else {
            log("Spieler 2 (gelb) ist am Zug...");
            if (ai_cb2.isSelected()) {
                kiAdvance(ai2);
            } else {
                playerAdvance();
            }
        }
    }

    private void kiAdvance(AI ai) {

        IntegerProperty tenthSeconds = new SimpleIntegerProperty();
        int millis = ai.getSettings().tLimit;
        progress_pb.progressProperty().bind(tenthSeconds.divide(millis / 200.0));  //Progress-Bar wandert in 1/5 Sekunden-Takt
        Timeline timeline = new Timeline(
                new KeyFrame(Duration.ZERO, new KeyValue(tenthSeconds, 0)),
                new KeyFrame(Duration.millis(millis), new KeyValue(tenthSeconds, millis / 200.0))
        );
        timeline.setCycleCount(Animation.INDEFINITE);

        task = new Task<>() {
            @Override
            protected Integer call() {
                return ai.advance(this);
            }
        };
        task.setOnSucceeded(t -> taskCompleted(ai, timeline));
        task.setOnCancelled(t -> taskCompleted(ai, timeline));

        //Starte Progress-Bar und AI-Task
        timeline.play();
        new Thread(task).start();

    }

    private void taskCompleted(AI ki, Timeline timeline) {
        timeline.stop();
        progress_pb.progressProperty().unbind();
        progress_pb.setProgress(0.0);
        try {
            //wir warten nochmal 1/10 Sekunde, um den Task zurückkehren zu lassen.
            Thread.sleep(100);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        int next = ki.getLastChoice();
        log("KI (" + (board.getTurn() ? "Spieler 1 (rot)" : "Spieler 2 (gelb)") + ") hat gespielt: " + (next + 1));
        log(">>> Die Position wurde bewertet mit: " + ki.getLastValue());
        board.add(next);
        advance();
    }

    private void playerAdvance() {
        inserts_hb.setVisible(true);
        inserts_hbt.setVisible(true);
        Paint color = Color.YELLOW;
        if (board.getTurn()) {
            color = Color.RED;
        }
        for (int i = 0; i < buttons.length; i++) {
            if (board.isFull(i)) buttons[i].setVisible(false);
            buttons[i].setFill(color);
        }
    }

    public void stop() {
        if (task != null && task.isRunning()) task.cancel(false);
        start_b.setDisable(false);
        reset_b.setDisable(true);
        stop_b.setDisable(true);
        start_b.setText("Fortsetzen");
        inserts_hb.setVisible(false);
        inserts_hbt.setVisible(false);
        for (int i = 0; i < buttons.length; i++) {
            if (board.isFull(i)) buttons[i].setVisible(true);
        }
        start_b.requestFocus();
    }

    public void reset() {
        stop();
        start_b.setText("Start");
        board = new Board();
        info_ta.setText(">");
        render();
    }

    private AISettings getSettings1() {
        AISettingsParameter s = new AISettingsParameter();
        s.maxDepth = Integer.parseInt(ki_md1.getText());
        s.numThreads = Integer.parseInt(ki_nt1.getText());
        s.tLimit = Integer.parseInt(ki_tl1.getText());
        s.useStatWeight = false;
        return new AISettings(s);
    }

    private AISettings getSettings2() {
        AISettingsParameter s = new AISettingsParameter();
        s.maxDepth = Integer.parseInt(ki_md2.getText());
        s.numThreads = Integer.parseInt(ki_nt2.getText());
        s.tLimit = Integer.parseInt(ki_tl2.getText());
        s.useStatWeight = true;
        return new AISettings(s);
    }

    private void render() {
        for (int i = 0; i < 7; i++) {
            for (int j = 0; j < 6; j++) {
                switch (board.get(i, j)) {
                    //anders als im Spielbrett zählt das Grid Y von oben nach unten
                    case EMPTY:
                        gridNodes[i][5 - j].setStyle("-fx-background-color: white");
                        break;
                    case RED:
                        gridNodes[i][5 - j].setStyle("-fx-background-color: red");
                        break;
                    case YELLOW:
                        gridNodes[i][5 - j].setStyle("-fx-background-color: yellow");
                        break;
                }
            }
        }
    }

    private void log(String s) {
        info_ta.appendText("\n" + "> " + s);
    }

}
