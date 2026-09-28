package co.edu.poli.dailysync.model;

/** Producto de la lista de compras (RF-06). */
public class ItemCompra {

    private long id;
    private String nombre;
    private String fotoUri;   // se usará en la siguiente entrega (cámara / galería)
    private boolean comprado;

    public ItemCompra(long id, String nombre) {
        this.id = id;
        this.nombre = nombre;
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
