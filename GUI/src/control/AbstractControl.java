package control;

public abstract class AbstractControl {

    private MainControl mainController;

    /**
     * Sets the main control of the object and calls the initialization method
     * propagates the parent for usage in child (for example updating the model)
     *
     * @param controller
     * @param parent
     */
    public void setMainController(MainControl controller, AbstractControl parent) {
        this.mainController = controller;
        controller.getRegisteredControls().put(this.getClass().getName(), this);
        this.afterSetMainController(parent);
    }

    public MainControl getMainController() {
        return mainController;
    }

    protected abstract void afterSetMainController(AbstractControl parent);

}
