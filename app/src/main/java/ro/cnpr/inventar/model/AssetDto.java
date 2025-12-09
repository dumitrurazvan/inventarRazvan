package ro.cnpr.inventar.model;

import java.io.Serializable;
import java.math.BigDecimal;

public class AssetDto implements Serializable {

    private static final long serialVersionUID = 1L;
    private long id;
    private String type;
    private String nrCrt;
    private String nrInventar;
    private String codObiect;
    private String denumireObiect;
    private String caracteristiciObiect;
    private BigDecimal valoareInventar;
    private String valoareInventarRaw;
    private String gestionarActual;
    private String compartimentGestionar;
    private String gestionarPrecedent;
    private String compartimentPrecedent;
    private String custodie;
    private String compartimentCustodie;
    private String propusCasare;
    private boolean propusCasareFlag;
    private boolean active;
    private boolean identified;
    private String locatia;
    private String camera;
    private String etaj;
    private Long roomId;
    private String roomDisplayName;

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getNrCrt() {
        return nrCrt;
    }

    public void setNrCrt(String nrCrt) {
        this.nrCrt = nrCrt;
    }

    public String getNrInventar() {
        return nrInventar;
    }

    public void setNrInventar(String nrInventar) {
        this.nrInventar = nrInventar;
    }

    public String getCodObiect() {
        return codObiect;
    }

    public void setCodObiect(String codObiect) {
        this.codObiect = codObiect;
    }

    public String getDenumireObiect() {
        return denumireObiect;
    }

    public void setDenumireObiect(String denumireObiect) {
        this.denumireObiect = denumireObiect;
    }

    public String getCaracteristiciObiect() {
        return caracteristiciObiect;
    }

    public void setCaracteristiciObiect(String caracteristiciObiect) {
        this.caracteristiciObiect = caracteristiciObiect;
    }

    public BigDecimal getValoareInventar() {
        return valoareInventar;
    }

    public void setValoareInventar(BigDecimal valoareInventar) {
        this.valoareInventar = valoareInventar;
    }

    public String getValoareInventarRaw() {
        return valoareInventarRaw;
    }

    public void setValoareInventarRaw(String valoareInventarRaw) {
        this.valoareInventarRaw = valoareInventarRaw;
    }

    public String getGestionarActual() {
        return gestionarActual;
    }

    public void setGestionarActual(String gestionarActual) {
        this.gestionarActual = gestionarActual;
    }

    public String getCompartimentGestionar() {
        return compartimentGestionar;
    }

    public void setCompartimentGestionar(String compartimentGestionar) {
        this.compartimentGestionar = compartimentGestionar;
    }

    public String getGestionarPrecedent() {
        return gestionarPrecedent;
    }

    public void setGestionarPrecedent(String gestionarPrecedent) {
        this.gestionarPrecedent = gestionarPrecedent;
    }

    public String getCompartimentPrecedent() {
        return compartimentPrecedent;
    }

    public void setCompartimentPrecedent(String compartimentPrecedent) {
        this.compartimentPrecedent = compartimentPrecedent;
    }

    public String getCustodie() {
        return custodie;
    }

    public void setCustodie(String custodie) {
        this.custodie = custodie;
    }

    public String getCompartimentCustodie() {
        return compartimentCustodie;
    }

    public void setCompartimentCustodie(String compartimentCustodie) {
        this.compartimentCustodie = compartimentCustodie;
    }

    public String getPropusCasare() {
        return propusCasare;
    }

    public void setPropusCasare(String propusCasare) {
        this.propusCasare = propusCasare;
    }

    public boolean isPropusCasareFlag() {
        return propusCasareFlag;
    }

    public void setPropusCasareFlag(boolean propusCasareFlag) {
        this.propusCasareFlag = propusCasareFlag;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public boolean isIdentified() {
        return identified;
    }

    public void setIdentified(boolean identified) {
        this.identified = identified;
    }

    public String getLocatia() {
        return locatia;
    }

    public void setLocatia(String locatia) {
        this.locatia = locatia;
    }

    public String getCamera() {
        return camera;
    }

    public void setCamera(String camera) {
        this.camera = camera;
    }

    public String getEtaj() {
        return etaj;
    }

    public void setEtaj(String etaj) {
        this.etaj = etaj;
    }

    public Long getRoomId() {
        return roomId;
    }

    public void setRoomId(Long roomId) {
        this.roomId = roomId;
    }

    public String getRoomDisplayName() {
        return roomDisplayName;
    }

    public void setRoomDisplayName(String roomDisplayName) {
        this.roomDisplayName = roomDisplayName;
    }
}
