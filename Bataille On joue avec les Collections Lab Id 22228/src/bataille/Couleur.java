package bataille;

public enum Couleur {
	//TODO fairecette classe enum , qui permet d'avoir toutes les couleur d'une carte
	CARREAU("carreau"), COEUR("coeur"), PIQUE("pique"), TREFLE("trefle");
	
	private String nom;
	
	
	private Couleur(String nom) {
		this.nom = nom;
	}

	public String getNom() {
		return nom;
	}
}
