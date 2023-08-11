package pl.edu.agh.aolesek.bts.trajectory.generator.utils;

public enum StaticCateroriesEnum {

    SCHOOL("school"),
    UNIVERSITY("university"),
    COMPANY("company"),
    SHOP("shop"),
    HOUSE("house");
    public final String label;

    private StaticCateroriesEnum(String label) {
        this.label = label;
    }
}
