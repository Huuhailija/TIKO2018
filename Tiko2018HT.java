/*
    Tietokantaohjelmoinnin harjoitustyö 2018
    by
    Teemu Soini
    Jere Huumonen
    Joona Hautalahti
 */

import java.sql.*;
import java.util.Scanner;

public class Tiko2018HT {
  //Test
  private static final String AJURI = "org.postgresql.Driver";
  private static final String PROTOKOLLA = "jdbc:postgresql:";
  private static final String PALVELIN = "dbstud2.sis.uta.fi";
  private static final int PORTTI = 5432;
  private static final String TIETOKANTA = "tiko2018r12";
  private static final String KAYTTAJA = "a660929";
  private static final String SALASANA = "teemu123";
  
  private static Connection con = null;
  
  public static void main(String[] args) {

    try {
      yhdista();
      kaynnista();
    } 
    catch (SQLException ex) {
      System.out.println("Tapahtui seuraava virhe: " + ex.getMessage());
    }
  }

  //Aloitusvalikko kirjautumiseen ja rekisteröitymiseen.
  public static void kaynnista() throws SQLException {
    
      System.out.println("TIKO 2018 Harjoitustyö\n");
      System.out.println("Tervetuloa keskusdivariin!\n");

      System.out.println("Kirjaudu sisään tai luo uusi käyttäjätili.");

      System.out.println("Kirjaudu sisään syöttämällä '1'");
      System.out.println("Luo uusi käyttäjätili syöttämällä '2'");
      System.out.println("Lisää teos tai nide syöttämällä '3'");

      Scanner sc = new Scanner(System.in);
      String syote = sc.next();
      while(!syote.equals("1") && !syote.equals("2") && !syote.equals("3")) {
        System.out.println("Virheellinen syöte!");
        syote = sc.nextLine();
      }
      if(syote.equals("1")) {
        kirjaudu();
      }
      else if(syote.equals("2")) {
        rekisteroidy();
      }
      else if(syote.equals("3")) {
        lisaaTeos();
      }
      sc.close();
  }
  
  //Uuden käyttäjän luonti
  public static void rekisteroidy() throws SQLException {
    
    String knimi;
    String salasana;
    String enimi;
    String snimi;
    String osoite;
    String pnum;

    Scanner sc = new Scanner(System.in);

    System.out.println("\nUuden käyttäjätilin luonti.");
    
    //Käyttäjänimen valinta
    System.out.println("Syötä käyttäjänimi:");
    knimi = sc.nextLine();
    
    //Do-while-rakenne tarkistaa onko käyttäjän syöttämä tunnus jo olemassa
    boolean sama = true;
    do {
      Statement stmt = con.createStatement();
      ResultSet rs = stmt.executeQuery("SELECT COUNT(*) AS lkm FROM kayttaja WHERE ktunnus = '" + knimi + "'");
      rs.next();

      //Jos tunnus löytyy tietokannasta, kysytään uusi käyttäjänimi
      if(rs.getInt("lkm") == 1) {
        System.out.println("Käyttäjänimi on jo olemassa!");
        System.out.println("Syötä uusi käyttäjänimi:");
        knimi = sc.nextLine();
      }
      else {
        sama = false;
      }
    }
    while(sama);
    
    //Salasanan valinta
    System.out.println("Syötä salasana:");
    salasana = sc.nextLine();
    
    //Etunimen valinta. Nimi saa sisältää vain aakkoset ja yhdysmerkin
    System.out.println("Syötä etunimi:");
    enimi = sc.nextLine();
    while(!enimi.matches("^[a-zA-ZöäåÖÄÅ-]*$")) {
      System.out.println("Virheellinen etunimi: Ainoastaan aakkoset ja \"-\" sallittu.");
      System.out.println("Syötä etunimi:");
      enimi = sc.nextLine();
    }
    
    //Sukunimen valinta. Nimi saa sisältää vain aakkoset ja yhdysmerkin
    System.out.println("Syötä sukunimi:");
    snimi = sc.nextLine();
    while(!snimi.matches("^[a-zA-ZöäåÖÄÅ-]*$")) {
      System.out.println("Virheellinen sukunimi: Ainoastaan aakkoset ja \"-\" sallittu.");
      System.out.println("Syötä sukunimi:");
      enimi = sc.nextLine();
    }
    
    //Osoitteen valinta.
    System.out.println("Syötä osoite:");
    osoite = sc.nextLine();
    
    //Puhelinnumeron valinta. Saa sisältää vain numeroita.
    System.out.println("Syötä puhelinnumero:");
    pnum = sc.nextLine();
    while(!pnum.matches("[0-9]+")) {
      System.out.println("Virheellinen puhelinnumero: Ainoastaan numerot 0-9 sallittu.");
      System.out.println("Syötä puhelinnumero:");
      enimi = sc.nextLine();
    }
    
    //Laskee käyttäjän id:n, joka on nykyisten käyttäjien määrä + 1
    Statement stmt = con.createStatement();
    ResultSet rs = stmt.executeQuery("SELECT COUNT(*) AS lkm FROM asiakas");
    rs.next();
    int id = 1 + rs.getInt("lkm");

    //Tallentaa uuden käyttäjän tietokantaan
    stmt.executeUpdate("INSERT INTO kayttaja VALUES('" + knimi + "', '" + salasana + "')");
    stmt.executeUpdate("INSERT INTO asiakas VALUES(" + id + ", '" + enimi + "', '" + snimi + "', '" + osoite + "', '" + pnum + "', '" + knimi + "')");
    
    sc.close();
  }
  
  //Kirjautuminen jo olemassa olevilla tunnuksilla
  public static void kirjaudu() throws SQLException {
    
    Scanner sc = new Scanner(System.in);
    
    System.out.println("\nSisäänkirjautuminen");
    
    //Katsoo löytyykö salasanaa ja käyttäjänimeä vastaava käyttäjä tietokannasta
    boolean loytyi = false;
    do {
      String kayttaja;
      System.out.println("Syötä käyttäjänimi:");
      kayttaja = sc.nextLine();

      String salasana;
      System.out.println("Syötä salasana:");
      salasana = sc.nextLine();
      
      Statement stmt;
      stmt = con.createStatement();
      ResultSet rs = stmt.executeQuery("SELECT COUNT(*) AS lkm FROM kayttaja WHERE ktunnus = '" + kayttaja + "' AND salasana = '" + salasana + "'");
      rs.next();
      if(rs.getInt("lkm") == 1) {
        loytyi = true;
      }
      else {
        System.out.println("Käyttäjänimi tai salasana on virheellinen.");
      }
    }
    while(!loytyi);
    sc.close();
  }
  
  //Ottaa yhteyden tietokantaan
  public static void yhdista() {
      try {
          Class.forName(AJURI);
      }
      catch (ClassNotFoundException e) {
          System.out.println(e.getMessage());
      }

      try {
      con=DriverManager.getConnection(PROTOKOLLA + "//" + PALVELIN + ":" + PORTTI + "/" + TIETOKANTA, KAYTTAJA, SALASANA); 
    }
    catch (SQLException e) {
      System.out.println("Tietokantayhteyden avaus ei onnistu");
      System.out.println(e.getMessage());
    }
  }

  //Lisätään teos tietokantaan
  public static void lisaaTeos() {
    Scanner sc = new Scanner(System.in);
    String knimi;
    String tekija;
    String luokka;
    String isbn;
    String vuosi;
    int nide_id;
    String hinta;
    String osto;
    String massa;
    boolean toista = false;
    System.out.println("Lisää teos tai nide tietokantaan:");

      System.out.println("Anna teoksen nimi.");
      knimi = sc.nextLine();
      // Tarkastetaan onko teos jo tietokannassa.
      try {
        Statement stmt;
        stmt = con.createStatement();
        ResultSet rs = stmt.executeQuery("SELECT nimi FROM teos WHERE nimi='" + knimi + "'");
        if (!rs.isBeforeFirst()) {
          System.out.println("Teosta ei ole tietokannassa. Tarvitaan lisätietoja.");
          System.out.println("Anna isbn:");
          isbn = sc.nextLine();
          System.out.println("Anna tekijä:");
          tekija = sc.nextLine();
          System.out.println("Anna vuosi:");
          vuosi = sc.nextLine();
          System.out.println("Anna luokka:");
          luokka = sc.nextLine();

          stmt.executeUpdate("INSERT INTO teos VALUES ('" + isbn +"','" + knimi + "','" + tekija + "','"+vuosi+ "','" + luokka + "')");
        }

        //Jos teos löytyy tietokannasta, niin lisätään nide, josta kysystään lisätietoja.
        System.out.println("Teos tietokannassa, lisätään nide.");
        System.out.println("Anna myynti hinta:");
        hinta = sc.nextLine();
        System.out.println("Anna osto hinta:");
        osto = sc.nextLine();
        System.out.println("Anna kirjan massa:");
        massa = sc.nextLine();
        do {
          //Jos niteitä on jo tietokannassa, niin kasvatetaan  nide_id:tä yhdellä.
          rs = stmt.executeQuery("SELECT * FROM nide");
          rs.next();
          nide_id = rs.getInt("nide_id") + 1;

          //Lisätään nide uudelle id:lle ja syötetään käyttäjän antamat arvot.
          stmt.executeUpdate("INSERT INTO nide VALUES ('" + nide_id + "','" + hinta + "','" + osto + "','" + massa + "')");
          System.out.println("Toistetaanko toiminto? Valitse kyllä jos haluat kopioida viime lisäyksen.");
          toista = sc.nextBoolean();
        }
        while (!toista);
      }
      catch (SQLException e) {
        System.out.println("Virhe! " + e.getMessage());
      }
      System.out.println("Haluatko lisätä toisen teoksen?");
      toista = sc.nextBoolean();
  }

  
  // K�ytt�j� hakee teosta
  public static void haeTeos() {
	  
	  Scanner sc = new Scanner(System.in);
	  int valinta;
	  String nimi;
	  String tekija;
	  String tyyppi;
	  String luokka;
	  
	  // Kysyt��n k�ytt�j�lt� mill� hakutermill� h�n teosta haluaa hakea.
	  System.out.println("Mill� haluat hakea teosta? 1. Teoksen nimell� 2. Teoksen tekij�ll� 3. Teoksen tyypill� 4. Teoksen luokalla");
	  valinta = sc.nextInt();
	  
	  boolean oikeaValinta = false;
	  do {
	  
		  if (valinta == 1) {
			  oikeaValinta = true;
			  System.out.println("Anna teoksen nimi:");
			  nimi = sc.nextLine();
		  
			  Statement stmt;
		  
			  try {
				  // Tehd��n kysely ja katsotaan, l�ytyyk� hakusanalla teosta.
				  stmt = con.createStatement();
				  PreparedStatement prstmt = con.prepareStatement("SELECT * FROM teos WHERE nimi LIKE ?");
				  prstmt.setString(1, "'%" + nimi + "%'");
			  
				  ResultSet rs = prstmt.executeQuery();
				  System.out.println("Tulokset:");
				  while(rs.next()) {
					  System.out.println(rs.getString("nimi") + ", " + rs.getString("tekija") + ", " + rs.getString("isbn")
					  						+ rs.getString("tyyppi") + ", " + rs.getString("luokka"));
				  
				  }	  
			  	}		
			  
			  catch (SQLException e) {
				  System.out.println("Virhe!" + e.getMessage());
			  
			  }
		 }
	  
		  if (valinta == 2) {
			  oikeaValinta = true;
			  System.out.println("Anna tekij�n nimi:");
			  tekija = sc.nextLine();
		  
			  Statement stmt;
		  
			  try {
				  // Tehd��n kysely ja katsotaan, l�ytyyk� hakusanalla teosta.
				  stmt = con.createStatement();
				  PreparedStatement prstmt = con.prepareStatement("SELECT * FROM teos WHERE tekij� LIKE ?");
				  prstmt.setString(1, "'%" + tekija + "%'");
			  
				  ResultSet rs = prstmt.executeQuery();
				  System.out.println("Tulokset:");
				  while(rs.next()) {
					  System.out.println(rs.getString("nimi") + ", " + rs.getString("tekija") + ", " + rs.getString("isbn")
					  						+ rs.getString("tyyppi") + ", " + rs.getString("luokka"));
				  
				  }	  
			  }
			  catch (SQLException e) {
				  System.out.println("Virhe!" + e.getMessage());
			  
			  }
		  }
	  
		  if (valinta == 3) {
			  oikeaValinta = true;
			  System.out.println("Anna teoksen tyyppi:");
			  tyyppi = sc.nextLine();
		  
			  Statement stmt;
		  
			  try {
				  // Tehd��n kysely ja katsotaan, l�ytyyk� hakusanalla teosta.
				  stmt = con.createStatement();
				  PreparedStatement prstmt = con.prepareStatement("SELECT * FROM teos WHERE tyyppi LIKE ?");
				  prstmt.setString(1, "'%" + tyyppi + "%'");
			  
				  ResultSet rs = prstmt.executeQuery();
				  System.out.println("Tulokset:");
				  while(rs.next()) {
					  System.out.println(rs.getString("nimi") + ", " + rs.getString("tekija") + ", " + rs.getString("isbn")
					  						+ rs.getString("tyyppi") + ", " + rs.getString("luokka"));
				  }	  
			  }
			  catch (SQLException e) {
				  System.out.println("Virhe!" + e.getMessage());
			  
			  }
		  }
		  
		  if (valinta == 4) {
			  oikeaValinta = true;
			  System.out.println("Anna teoksen luokka:");
		  	luokka = sc.nextLine();
		  
		  	Statement stmt;
		  
		  	try {
		  		// Tehd��n kysely ja katsotaan, l�ytyyk� hakusanalla teosta.
		  		stmt = con.createStatement();
		  		PreparedStatement prstmt = con.prepareStatement("SELECT * FROM teos WHERE luokka LIKE ?");
		  		prstmt.setString(1, "'%" + luokka + "%'");
			  
		  		ResultSet rs = prstmt.executeQuery();
		  		System.out.println("Tulokset:");
		  		while(rs.next()) {
				  System.out.println(rs.getString("nimi") + ", " + rs.getString("tekija") + ", " + rs.getString("isbn")
				  						+ rs.getString("tyyppi") + ", " + rs.getString("luokka"));
				  
		  		}	  
		  	}
		  	catch (SQLException e) {
		  		System.out.println("Virhe!" + e.getMessage());
			  
		  	}
		  }
	  
	  	}
	  
	  while (!oikeaValinta);
	 
	  
  }
}
