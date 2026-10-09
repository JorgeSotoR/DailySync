package co.edu.poli.dailysync.model;

/** Producto de la lista de compras (RF-06). Puede tener una foto de la cámara, la galería u otra app. */
public class ItemCompra {

    private long id;
    private String nombre;
    private String fotoUri;   // ruta del archivo en el almacenamiento interno, o null
    private boolean comprado;

    public ItemCompra(long id, String nombre, String fotoUri) {
        this.id = id;
        this.nombre = nombre;
        this.fotoUri = fotoUri;
        this.comprado = false;
    }

    public boolean tieneFoto() {
        return fotoUri != null && !fotoUri.isEmpty();
    }

    public long getId() { return id; }
    public String getNombre() { return nombre; }
    public String getFotoUri() { return fotoUri; }
    public boolean isComprado() { return comprado; }
    public void setComprado(boolean comprado) { this.comprado = comprado; }
}
