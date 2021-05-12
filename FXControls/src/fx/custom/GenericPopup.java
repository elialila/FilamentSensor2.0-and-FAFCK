package fx.custom;

import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.ImageView;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.WindowEvent;

import java.io.IOException;

/**
 * Works only with <? extends Pane> and SplitPane
 */
public class GenericPopup {
    @FXML
    private VBox root;

    private Parent previousParent;
    private Node node;


    public Parent getPreviousParent() {
        return previousParent;
    }

    public void setPreviousParent(Parent previousParent) {
        this.previousParent = previousParent;
    }

    public Node getNode() {
        return node;
    }

    public void setNode(Node node) {
        this.node = node;
    }

    public VBox getRoot() {
        return root;
    }

    public void initialize() {
        if (getNode() != null) {
            getRoot().getChildren().clear();
            getRoot().getChildren().add(getNode());
        }

    }


    public static void openPopup(String title, ImageView iv) throws IOException {
        FXMLLoader loader = new FXMLLoader(GenericPopup.class.getResource("/fx/custom/view/GenericPopup.fxml"));

        Scene dialogScene = new Scene(loader.load());

        GenericPopup controller = loader.getController();
        controller.setNode(iv);

        iv.fitWidthProperty().bind(controller.root.widthProperty());
        iv.fitHeightProperty().bind(controller.root.heightProperty());

        Stage inputStage = new Stage();
        inputStage.setWidth(iv.getImage().getWidth());
        inputStage.setHeight(iv.getImage().getHeight());
        inputStage.setTitle(title);
        inputStage.initModality(Modality.WINDOW_MODAL);
        inputStage.setScene(dialogScene);
        controller.initialize();
        inputStage.show();
    }

    public static void openPopup(String title, StackImageView siv, EventHandler<WindowEvent> onClose) throws IOException {
        FXMLLoader loader = new FXMLLoader(GenericPopup.class.getResource("/fx/custom/view/GenericPopup.fxml"));
        Scene dialogScene = new Scene(loader.load());
        GenericPopup controller = loader.getController();
        controller.setNode(siv);

        siv.prefWidthProperty().bind(controller.root.widthProperty());
        siv.prefHeightProperty().bind(controller.root.heightProperty());

        Stage inputStage = new Stage();
        inputStage.setWidth(500);
        inputStage.setHeight(500);
        inputStage.setTitle(title);
        inputStage.initModality(Modality.WINDOW_MODAL);
        inputStage.setScene(dialogScene);

        inputStage.setOnCloseRequest(onClose);
        controller.initialize();
        inputStage.show();

    }


    /**
     * @param title
     * @param node
     * @param previousParent
     * @param origPosition   if <0 the value will be ignored
     * @throws IOException
     */
    public static void openPopup(String title, Node node, Parent previousParent, int origPosition) throws IOException {

        FXMLLoader loader = new FXMLLoader(GenericPopup.class.getResource("/fx/custom/view/GenericPopup.fxml"));

        Scene dialogScene = new Scene(loader.load());

        GenericPopup controller = loader.getController();
        controller.setNode(node);
        controller.setPreviousParent(previousParent);

        Stage inputStage = new Stage();
        inputStage.setTitle(title);
        inputStage.initModality(Modality.WINDOW_MODAL);
        inputStage.setScene(dialogScene);
        controller.initialize();
        inputStage.show();

        inputStage.setOnCloseRequest(event -> {
            controller.getRoot().getChildren().clear();
            if (origPosition < 0) {
                if (controller.getPreviousParent() instanceof SplitPane) {
                    ((SplitPane) controller.getPreviousParent()).getItems().add(controller.getNode());
                    //maybe the original position in list would be good?
                } else if (controller.getPreviousParent() instanceof Pane) {
                    ((Pane) controller.getPreviousParent()).getChildren().add(controller.getNode());
                } else if (controller.getPreviousParent() instanceof TabPane) {
                    //do something or remove it?
                } else if (controller.getPreviousParent() instanceof ScrollPane) {
                    ((ScrollPane) controller.getPreviousParent()).setContent(controller.getNode());
                }

            } else {
                if (controller.getPreviousParent() instanceof SplitPane) {
                    ((SplitPane) controller.getPreviousParent()).getItems().add(origPosition, controller.getNode());
                    //maybe the original position in list would be good?
                } else if (controller.getPreviousParent() instanceof Pane) {
                    ((Pane) controller.getPreviousParent()).getChildren().add(origPosition, controller.getNode());
                } else if (controller.getPreviousParent() instanceof TabPane) {
                    //do something or remove it?
                } else if (controller.getPreviousParent() instanceof ScrollPane) {
                    ((ScrollPane) controller.getPreviousParent()).setContent(controller.getNode());
                }
            }
        });


    }


}
