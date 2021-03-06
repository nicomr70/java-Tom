package quincaillerie;

import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Random;

import javax.swing.JOptionPane;

import clients.Client;
import clients.Entreprise;
import clients.Particulier;
import commandes.Commande;
import commandes.Facture;
import pieces.Piece;
import pieces.PieceCompositeEnKit;
import pieces.PieceCompositeMontee;
import pieces.PieceDeBase;
import servicesBancaires.ServiceBancaire;

public class Quincaillerie {
	
	private String nom;
	private double tresorerie;
	private Catalogue catalogue;
	private Stocks stocks;
	private Map<Client, LinkedHashSet<Commande>> listeClientsCommandes;

    public Quincaillerie(String nom, double tresorerie, Catalogue catalogue, Stocks stocks, Map<Client, Collection<Facture>> listeClientsFactures) {
		setNom(nom);
		setTresorerie(tresorerie);
		setCatalogue(catalogue);
		setStocks(stocks);
		setListeClientsCommandes();
	}
    
	public String getNom() {
		return nom;
	}
	public void setNom(String nom) {
		this.nom = nom;
	}
	
	public double getTresorerie() {
		return tresorerie;
	}
	public void setTresorerie(double tresorerie) {
		this.tresorerie = tresorerie;
	}
	
	public Catalogue getCatalogue() {
		return catalogue;
	}
	public void setCatalogue(Catalogue catalogue) {
		this.catalogue = catalogue;
	}

	public Stocks getStocks() {
		return stocks;
	}
	public void setStocks(Stocks stocks) {
		this.stocks = stocks;
	}

	public Map<Client, LinkedHashSet<Commande>> getListeClientsCommandes() {
		return listeClientsCommandes;
	}
	public void setListeClientsCommandes() {
		listeClientsCommandes = new HashMap<>();
	}

	/**
	 * Retourne le numéro de la prochaine facture à créer, basé sur le nombre de factures déjà éditées
	 * @return int le numéro de la prochaine facture
	 */
	private int numCommande() {
		int nb = 0;
		for(LinkedHashSet<Commande> listesCommandes : listeClientsCommandes.values()) {
			nb += listesCommandes.size();
		}
		return nb+1;
	}
	
	/**
	 * Retourne l'id du prochain client à créer, basé sur le nombre de clients déjà enregistrés et son type (particulier ou entreprise
	 * @param part {@linkplain Boolean} true si le client est un particulier, faux sinon (ie si le client est une entreprise)
	 * @return String l'id du prochain client
	 */
	public String idNouveauClient(Boolean part) {
		int nb = listeClientsCommandes.keySet().size() + 1;
		String id = "";
		if(nb>0 && nb<1000) id += String.format("%04d", nb);	//ici je suppose qu'on ne dépassera jamais 1000 clients, pas réaliste mais ça correspond aux demandes pour la ref client
		if(part) {
			id += "PA";
		}else {
			id += "EN";
		}
		Random rn = new Random();
		int r = rn.nextInt(100);
		id += String.format("%02d", r);
		return id;
	}
	
	public String refNewPiece(String nom, int typePiece) {

		String ref = "";		
		if(typePiece == 0) {
			ref += "00";
		}else if(typePiece == 1){
			ref += "01";
		}else {
			ref += "02";
		}
		
		String[] partiesNom = nom.split(" ");	
		if(partiesNom.length == 1) {
			ref += partiesNom[0].substring(0, 2).toUpperCase();
		}else {
			ref += partiesNom[0].substring(0, 1).toUpperCase();
			ref += partiesNom[partiesNom.length-1].substring(0, 1).toUpperCase();
		} 

		ref += String.format("%02d", countPieceRef(ref)+1);
		
		return ref;
	}
	
	private int countPieceRef(String partialRef) {
		int c = 0;
		for(Piece p : catalogue.catalogue) {
			if( partialRef.equals( p.getRef().substring(0, 4))) c++;
		}
		return c;
	}
	
	/**
	 * Vérifie si un email est connu par la quincaillerie, i.e. si un client l'utilise déjà
	 * @param mail {@linkplain String} le mail à vérifier
	 * @return true si le mail est connu (déjà utilisé), false sinon
	 */
	public boolean mailConnu(String mail) {
		boolean connu = false;
		Iterator<Client> it = listeClientsCommandes.keySet().iterator();
		while(it.hasNext() && !connu) {
			if(mail.equals(it.next().getEmail())) connu = true;
		}
		return connu;
	}
	
	/**
	 * Permet à un client de se connecter 
	 * @param mail {@linkplain String} l'email du client souhaitant se connecter
	 * @param password {@linkplain String} le mot de passe du client (ici le password root permet de se connecter
	 * @return {@linkplain Client} le client si celui-çi a réussi à se connecter, null sinon
	 */
	public Client connexionClient(String mail, String password) {
		Client client = null;
		Iterator<Client> it = listeClientsCommandes.keySet().iterator();
		while(it.hasNext() && client == null) {
			Client c = it.next();
			if(mail.equals(c.getEmail())) client = c;
		}
		return (client != null && password.equals("root") ? client : null);
	}
	
	/**
	 * Calcule le prix de la commande en fonctions des pièces et du nombre d'exemplaires qui la composent
	 * @param listePiecesExemplaires {@link Map} la liste des pièces qui composent la commande
	 * @return {@link Double} le prix de la commande
	 */
	public double calculPrixCommande(Map<Piece, Integer> listePiecesExemplaires) {
		double prix = 0;
		for(Piece p : listePiecesExemplaires.keySet()) {
			if(p instanceof PieceCompositeEnKit) {
				prix += ((PieceCompositeEnKit) p).prix() * listePiecesExemplaires.get(p);
			}else if(p instanceof PieceCompositeMontee) {
				prix += ((PieceCompositeMontee) p).prix() * listePiecesExemplaires.get(p);
			}else if(p instanceof PieceDeBase) {
				prix += ((PieceDeBase) p).prix() * listePiecesExemplaires.get(p);
			}
		}
		return prix;
	}
	
	/**
	 * Vérifie l'existence d'un client dans la liste des clients de la quincaillerie
	 * @param client {@link Client} dont on veut vérifier l'existence
	 * @return {@link Boolean} true si le client est connu de la quincaillerie, false sinon
	 */
	public boolean clientConnu(Client client) {
		return listeClientsCommandes.containsKey(client);
	}
	
	/**
	 * Vérifie si la quincaillerie possède suffisement de stocks de chaque pièces de la liste donnée en paramètre
	 * @param listeArticles {@link Map} la liste des pièces et nombre d'exemplaires de chaque pièces
	 * @return stockSuff {@link Boolean} true si la quincaillerie a des stocks suffisants, false sinon
	 */
	public boolean stocksSuffisants(Map<Piece, Integer> listeArticles) {
		boolean stockSuff = true;
		Iterator<Piece> it = listeArticles.keySet().iterator();
		while(it.hasNext() && stockSuff) {
			Piece p = it.next();
			if(stocks.stocksPiece(p) < listeArticles.get(p)) stockSuff = false;
			
		}
		return stockSuff;
	}
	
	/**
	 * Ajoute un client dans la listeClientsCommandes de la quincaillerie, et crée un set vide pour ses commandes
	 * @param client {@link Client} le client à ajouter
	 */
	public void ajouterClient(Client client) {
		listeClientsCommandes.putIfAbsent(client, new LinkedHashSet<>());
	}
	
	/**
	 * Ajoute une commande à la liste des commandes du client passé en paramètre dans la listeClientsCOmmandes de la quincaillerie, uniquement
	 * si le client est déjà connu de la quincaillerie 
	 * @param client {@link Client} le client auquel on ajoute la commande
	 * @param commande {@link Commande} la commande à ajouter au client
	 */
	public void ajouterCommandeClient(Client client, Commande commande) {
		if(listeClientsCommandes.get(client).size() == 0) {
			LinkedHashSet<Commande> lp = new LinkedHashSet<>();
			lp.add(commande);
			listeClientsCommandes.get(client).addAll(lp);
		}else {
			listeClientsCommandes.get(client).add(commande);
		}
		
	}
	
	public LinkedHashSet<Commande> getCommandesClient(Client c){
		return listeClientsCommandes.get(c);
	}
	
	/**
	 * Recherche une commande d'un client suivant son numéro de commande
	 * @param client {@link Client} le client dont on souhaite cherche la commande
	 * @param numCommande {@link Integer} le numéro de la commande recherchée
	 * @return {@link Boolean} true si la commande a été trouvée, false sinon
	 */
	public Commande rechercheCommandeClient(Client client, int numCommande) {
		Commande commande = null;
		Iterator<Commande> it = listeClientsCommandes.get(client).iterator();
		while(it.hasNext() && commande == null) {
			Commande cInt = it.next();
			if(cInt.getNum() == numCommande) {
				commande = cInt;
			}
		}
		return commande;
	}
	
	/**
	 * Crée une commande pour un client. Une commande ne peut être passée que sous 3 conditions:<br>
	 * &emsp; * le client est connu de la quincaillerie<br>
	 * &emsp; * la quincaillerie possède suffisement de stocks pour couvrir la commande<br>
	 * &emsp; * le client a suffisement de crédit pour acheter les pièces de la commande<br>
	 * @param client {@link Client} le client qui passe la commande
	 * @param listePiecesExemplaires {@link Map} la liste des pièces et leur nombre d'exemplaires que le client veut acheter
	 * @return {@link Commande} null si la commande n'a pas pu être passée, la commande créée sinon
	 */
	public Commande creationCommande(Client client, Map<Piece, Integer> listePiecesExemplaires) {
		Commande commande = null;
		double prixCommande = calculPrixCommande(listePiecesExemplaires);
		prixCommande *= (client instanceof Particulier && ((Particulier) client).isFidelite() ? 0.9 : 1); 
		if(!clientConnu(client)) {
			JOptionPane.showMessageDialog(null, "Client inconnu");
		}else if(!stocksSuffisants(listePiecesExemplaires)) {
			JOptionPane.showMessageDialog(null, "Stocks insuffisants");
		}else if(ServiceBancaire.prelevementCreditClient(client, prixCommande)) {
			Map<Piece, Integer> lpe = copieListePieces(listePiecesExemplaires);
			
			if(client instanceof Particulier) {
				commande = new Commande(numCommande(), getNom(), copieClientParticulier((Particulier) client), new Date(), lpe, prixCommande);
			}else {
				commande = new Commande(numCommande(), getNom(), copieClientEntreprise((Entreprise) client), new Date(), lpe, prixCommande);
			}
			
			ajouterCommandeClient(client, commande);
			ServiceBancaire.approvisionneTresorerieQuincaillerie(this, prixCommande);
			
		}
		return commande;
	}
	
	private Map<Piece, Integer> copieListePieces(Map<Piece, Integer> map){
		Map<Piece, Integer> lpe = new HashMap<>();
		for(Piece p : map.keySet()) {
			lpe.put(p, map.get(p));
		}
		return lpe;	
	}
	
	private Particulier copieClientParticulier(Particulier particulier) {
		return new Particulier(particulier.getId(), particulier.getAdresse(), particulier.getTel(), particulier.getEmail(), 
								particulier.getCredit(), ((Particulier)particulier).getCivilite(), ((Particulier)particulier).getNom(), 
								((Particulier)particulier).getPrenom(), ((Particulier)particulier).isFidelite());
	}
	
	private Entreprise copieClientEntreprise(Entreprise entreprise) {
		return new Entreprise(entreprise.getId(), entreprise.getAdresse(), entreprise.getTel(), entreprise.getEmail(), 
				entreprise.getCredit(), ((Entreprise)entreprise).getSiegeSocial(), ((Entreprise)entreprise).getNomCommercial(), 
				((Entreprise)entreprise).getCategorie());
	}
	
	public void afficheClients() {
		for(Client c : listeClientsCommandes.keySet()) {
			System.out.println(c + "\n");
		}
	}
	
}
