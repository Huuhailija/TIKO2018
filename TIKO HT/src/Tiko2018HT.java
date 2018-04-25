/*
    Tietokantaohjelmoinnin harjoitustyö 2018
    by
    Teemu Soini
    Jere Huumonen
    Joona Hautalahti
 */

import java.sql.*;
import java.util.Scanner;
import java.util.Calendar;
public class Tiko2018HT {
  //Yhteys tietokantaan
  //private static final String AJURI = "org.postgresql.Driver";
  private static final String PROTOKOLLA = "jdbc:postgresql:";
  private static final String PALVELIN = "localhost";
  private static final int PORTTI = 5432;
  private static final String TIETOKANTA = "postgres";
  private static final String KAYTTAJA = "postgres";
  private static final String SALASANA = "";
  
  private static Connection con = null;
  
  private static Scanner sc = new Scanner(System.in);
  
  //Sisäänkirjautunut käyttäjä
  private static final String KESKUS = "Keskusdivari";
  private static final String D1 = "Divari D1";
  
  private static String kayttaja = "";
  private static boolean onYllapitaja = false;
  private static boolean keskus_oikeudet = false;
  private static boolean d1_oikeudet = false;
  
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
      
      Statement stmt = con.createStatement();
      stmt.execute("SET SEARCH_PATH TO keskusdivari");

      String syote = sc.nextLine();
      while(!syote.equals("1") && !syote.equals("2")) {
        System.out.println("Virheellinen syöte!");
        syote = sc.nextLine();
      }
      if(syote.equals("1")) {
        kirjaudu();
      }
      else if(syote.equals("2")) {
        rekisteroidy();
      }
  }
  
  //Uuden käyttäjän luonti
  public static void rekisteroidy() throws SQLException {
    
    String knimi;
    String salasana;
    String enimi;
    String snimi;
    String osoite;
    String pnum;

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
    
    kayttaja = knimi;
    avaaEtusivu();
  }
  
  //Kirjautuminen jo olemassa olevilla tunnuksilla
  public static void kirjaudu() throws SQLException {
    
    System.out.println("\nSisäänkirjautuminen");
    
    //Katsoo löytyykö salasanaa ja käyttäjänimeä vastaava käyttäjä tietokannasta
    String knimi;
    boolean loytyi = false;
    do {

      System.out.println("Syötä käyttäjänimi:");
      knimi = sc.nextLine();

      String salasana;
      System.out.println("Syötä salasana:");
      salasana = sc.nextLine();
      Statement stmt;
      stmt = con.createStatement();
      ResultSet rs = stmt.executeQuery("SELECT COUNT(*) AS lkm FROM kayttaja WHERE ktunnus = '" + knimi + "' AND salasana = '" + salasana + "'");
      rs.next();
      if(rs.getInt("lkm") == 1) {
        loytyi = true;
      }
      else {
        System.out.println("Käyttäjänimi tai salasana on virheellinen.");
      }
    }
    while(!loytyi);
    
    //Otetaan ylös käyttäjänimi
    kayttaja = knimi;
    
    //Katsotaan onko käyttäjä ylläpitäjä ja mihin tietokantoihin sillä on oikeus
    Statement stmt = con.createStatement();
    ResultSet rs = stmt.executeQuery("SELECT COUNT(*) AS lkm FROM yllapitaja WHERE ktunnus = '" + knimi + "'");
    rs.next();
    if(rs.getInt("lkm") == 1) {
      onYllapitaja = true;
      rs = stmt.executeQuery("SELECT yllapitaja_id FROM yllapitaja WHERE ktunnus = '" + knimi + "'");
      rs.next();
      int id = rs.getInt("yllapitaja_id");
      rs = stmt.executeQuery("SELECT divari_nimi FROM divari WHERE yllapitaja_id = " + id);
      while(rs.next()) {
        String divari = rs.getString("divari_nimi");
        if(divari.equals("D1")) {
          d1_oikeudet = true;
          keskus_oikeudet = true;
        }
        else if(divari.equals("D2")) {
          keskus_oikeudet = true;
        }
      }
    }
    
    avaaEtusivu();
  }
  
  //Ottaa yhteyden tietokantaan
  public static void yhdista() {
    /*try {
          Class.forName(AJURI);
    }
    catch (ClassNotFoundException e) {
          System.out.println(e.getMessage());
    }
    */
    try {
      con=DriverManager.getConnection(PROTOKOLLA + "//" + PALVELIN + ":" + PORTTI + "/" + TIETOKANTA, KAYTTAJA, SALASANA);
    }
    catch (SQLException e) {
      System.out.println("Tietokantayhteyden avaus ei onnistu");
      System.out.println(e.getMessage());
    }
  }
  
  //Etusivu
  public static void avaaEtusivu() throws SQLException {
    //Tulostetaan käyttäjän nimi ja rooli
    System.out.println("\nTervetuloa sisään " + kayttaja + "\n");
    if(!onYllapitaja) {
      System.out.println("Kayttäjäroolisi on: Asiakas");
    }
    else {
      System.out.println("Käyttäjäroolisi on: Ylläpitaja");
      if(keskus_oikeudet || d1_oikeudet) {
        System.out.print("Sinulla on oikeudet seuraaviin tietokantoihin: ");
        if(keskus_oikeudet) {
          System.out.print("Keskustietokanta");
        }
        if(d1_oikeudet) {
          System.out.print(", D1");
        }
      }
      System.out.println("\n");
    }
    
    //Lista mahdollisista komennoista riippuen roolista
    boolean lopeta = false;
    while(!lopeta) {
      System.out.println("Etusivu\nValitse komento syöttämällä komentoa vastaava numero:");
      System.out.println("[0] Kirjaudu ulos");
      if(onYllapitaja) {
        System.out.println("[1] Lisää teos keskustietokantaan");
        if(d1_oikeudet) {
          System.out.println("[2] Lisää teos D1 tietokantaan");
          System.out.println("[3] Tulosta raportti R1 keskusdivarista");
          System.out.println("[4] Tulosta raportti R2 keskusdivarista");
          System.out.println("[5] Tulosta raportti R1 divarista D1");
          System.out.println("[6] Tulosta raportti R2 divarista D1");
        }
        else {
          System.out.println("[2] Tulosta raportti R1 keskusdivarista");
          System.out.println("[3] Tulosta raportti R2 keskusdivarista");
        }
      }
      else
        System.out.println("[1] Tilaa kirja");
      
      //Luetaan käyttäjän antama komento ja jatketaan riippuen komennosta
      String komento;
      komento = sc.nextLine();
      //Mikä tahansa numero 0-9
      if(komento.matches("\\d")) {
        switch(komento) {
          case "0":
            lopeta = true;
            break;
          case "1":
            if(!onYllapitaja) {
              haeTeos();
            }
            else if(onYllapitaja) {
              lisaaTeos(KESKUS);
            }
            break;
          case "2":
            if(onYllapitaja) {
              if(d1_oikeudet) {
                lisaaTeos(D1);
              }
              else {
                tulostaR1(KESKUS);
              }
            }
            else {
              System.out.println("Komentoa ei ole olemassa");
            }
            break;
          case "3":
            if(onYllapitaja) {
              if(d1_oikeudet) {
                tulostaR1(KESKUS);
              }
              else {
                tulostaR2(KESKUS);
              }
            }
            else {
              System.out.println("Komentoa ei ole olemassa");
            }
            break;
          case "4":
            if(onYllapitaja && d1_oikeudet) {
              tulostaR2(KESKUS);
            }
            else {
              System.out.println("Komentoa ei ole olemassa");
            }
            break;
          case "5":
            if(onYllapitaja && d1_oikeudet) {
              tulostaR1(D1);
            }
            else {
              System.out.println("Komentoa ei ole olemassa");
            }
            break;
          case "6":
            if(onYllapitaja && d1_oikeudet) {
              tulostaR2(D1);
            }
            else {
              System.out.println("Komentoa ei ole olemassa");
            }
            break;
          default:
            System.out.println("Komentoa ei ole olemassa!\n");
            break;
        }
      }
      else {
        System.out.println("Virheellinen komento!");
      }
    }
    System.out.println("\nOlet kirjautunut ulos\n");
    
  }
  
  //Tulostaa näytölle raportin R1
  public static void tulostaR1(String tietokanta) throws SQLException {
    
    //Valitaan mitä tietokantaa(schemaa) käytetään
    if(tietokanta.equals(KESKUS)) {
      Statement stmt = con.createStatement();
      stmt.execute("SET SEARCH_PATH TO keskusdivari");
    }
    else if(tietokanta.equals(D1)) {
      Statement stmt = con.createStatement();
      stmt.execute("SET SEARCH_PATH TO d1");
    }    

    System.out.println("\nLuetellaan teokset jotka täsmäävät hakusanaan");
    
    //Samalla kertaa voi tehdä useamman kyselyn eri hakusanoilla
    boolean jatka = true;
    while(jatka) {
      System.out.println("Syötä hakusana:");
      String hakusana = sc.nextLine();

      //Määrittää kuinka monta hakutulosta saatiin
      PreparedStatement lkm = con.prepareStatement("SELECT COUNT(*) AS lkm FROM teos WHERE LOWER(nimi) LIKE ? OR LOWER(tekija) LIKE ? OR LOWER(tyyppi) LIKE ? OR LOWER(luokka) LIKE ?");
      //Määrittää kaikki haun tulokset
      PreparedStatement tulokset = con.prepareStatement("SELECT * FROM teos WHERE LOWER(nimi) LIKE ? OR LOWER(tekija) LIKE ? OR LOWER(tyyppi) LIKE ? OR LOWER(luokka) LIKE ?");
      tulokset.setString(1, "%" + hakusana + "%");
      tulokset.setString(2, "%" + hakusana + "%");
      tulokset.setString(3, "%" + hakusana + "%");
      tulokset.setString(4, "%" + hakusana + "%");
      lkm.setString(1, "%" + hakusana + "%");
      lkm.setString(2, "%" + hakusana + "%");
      lkm.setString(3, "%" + hakusana + "%");
      lkm.setString(4, "%" + hakusana + "%");
      ResultSet rs = lkm.executeQuery();
      rs.next();
      System.out.println("\nHakusanalla \"" + hakusana + "\" löytyi yhteensä " + rs.getString("lkm") + " tulosta:");
      rs = tulokset.executeQuery();
      while(rs.next()) {
        System.out.print(rs.getString("tekija") + ", " + rs.getString("nimi") + ", ");
        if(rs.getString("isbn") != null) {
          System.out.print(rs.getString("isbn") + ", ");
        }
        System.out.println(rs.getString("tyyppi") + ", " + rs.getString("luokka"));
      }
      //Haluaako käyttäjä tehdä uuden haun
      System.out.println("\n[1] Tee uusi haku | [2] Palaa etusivulle");
      String syote = sc.nextLine();
      if(!syote.equals("1")) {
        jatka = false;
        System.out.println("Palataan etusivulle\n");
      }
    }
    Statement stmt = con.createStatement();
    stmt.execute("SET SEARCH_PATH TO keskusdivari");
    
  }
  
  //tulostaa näytölle raportin R2
  public static void tulostaR2(String tietokanta) throws SQLException {
    
    //Valitaan mitä tietokantaa(schemaa) käytetään
    if(tietokanta.equals(KESKUS)) {
      Statement stmt = con.createStatement();
      stmt.execute("SET SEARCH_PATH TO keskusdivari");
    }
    else if(tietokanta.equals(D1)) {
      Statement stmt = con.createStatement();
      stmt.execute("SET SEARCH_PATH TO d1");
    }      
 
    System.out.println("Tulostetaan raportti eri luokkien sisältämistä teoksista ja niiden kokonaismyyntihinnat ja keskihinnat");
    System.out.println("\n------------------------------------------------------");
    
    //Uloin silmukka hakee kaikki luokat ja käy ne läpi yksi kerrallaan
    PreparedStatement luokkaHaku = con.prepareStatement("SELECT DISTINCT luokka FROM teos");
    ResultSet luokat = luokkaHaku.executeQuery();
    while(luokat.next()) {
      String luokka = luokat.getString("luokka");
      System.out.println("Luokka: " + luokka +"\nLuokan " + luokka + " sisältämät teoket:\n");
      PreparedStatement teosHaku = con.prepareStatement("SELECT nimi, teos_id FROM teos WHERE luokka = ?");
      teosHaku.setString(1, luokka);
      ResultSet teokset = teosHaku.executeQuery();
      
      //Keskimmäinen silmukka hakee kaikki luokan teokset ja käy ne läpi kerrallaan
      //Jokaisesta teoksesta kerätään sen sisältämien niteiden summa ja lukumäärä
      double summa = 0;
      int lukumaara = 0;
      
      while(teokset.next()) {
        System.out.println(teokset.getString("nimi"));
        int id = teokset.getInt("teos_id");
        PreparedStatement nideHaku = con.prepareStatement("SELECT SUM(hinta) AS summa, COUNT(*) AS lkm FROM nide WHERE teos_id = ?");
        nideHaku.setInt(1, id);
        ResultSet niteet = nideHaku.executeQuery();
        
        //Sisimmäinen silmukka käy läpi kaikki teoksen sisältämät niteet ja
        //laskee niteiden summan ja lukumäärän ja lisää ne muuttujiin
        while(niteet.next()) {
          if(niteet.getString("summa") != null) {
            summa += Double.parseDouble(niteet.getString("summa"));
          }
          lukumaara += Integer.parseInt(niteet.getString("lkm"));
        }
      }
      
      //Lopuksi tulostetaan luokan teosten niteiden lukumäärä, summa ja keskihinta
      System.out.println("\nTeosten niteiden yhteislukumäärä: " + lukumaara);
      System.out.println("Teosten niteiden yhteismyyntihinta: " + summa);
      if(summa > 0)
        System.out.println("Teosten niteiden keskihinta: " + summa / lukumaara);
      else
        System.out.println("Teosten niteiden keskihinta: 0");
      System.out.println("\n------------------------------------------------------\n");
    }
    System.out.println("Palaa etusivulle syöttämällä mitä tahansa:");
    sc.nextLine();
    
    Statement stmt = con.createStatement();
    stmt.execute("SET SEARCH_PATH TO keskusdivari");
    
  }

  //Lisätään teos tietokantaan
  public static void lisaaTeos(String tietokanta) throws SQLException {
    
    //Valitaan mitä tietokantaa(schemaa) käytetään
    if(tietokanta.equals(KESKUS)) {
      Statement stmt = con.createStatement();
      stmt.execute("SET SEARCH_PATH TO keskusdivari");
    }
    else if(tietokanta.equals(D1)) {
      Statement stmt = con.createStatement();
      stmt.execute("SET SEARCH_PATH TO d1");
    }
    
    String knimi;
    String tekija;
    String tyyppi;
    String luokka;
    String isbn;
    String vuosi;
    int id;
    boolean toista = false;
    System.out.println("Lisää teos tai nide tietokantaan:");
    do {
        System.out.println("Anna teoksen nimi");
        knimi = sc.nextLine();
        // Tarkastetaan onko teos jo tietokannassa.
        try {
            Statement stmt;
            stmt = con.createStatement();
            ResultSet rs = stmt.executeQuery("SELECT * FROM teos WHERE nimi='" + knimi + "'");
            if (!rs.isBeforeFirst()) {
                System.out.println("Teosta ei ole tietokannassa. Tarvitaan lisätietoja.");
                System.out.println("Anna isbn:");
                isbn = sc.nextLine();
                System.out.println("Anna tekijä:");
                tekija = sc.nextLine();
                System.out.println("Anna vuosi:");
                vuosi = sc.nextLine();
                System.out.println("Anna tyyppi:");
                tyyppi = sc.nextLine();
                System.out.println("Anna luokka:");
                luokka = sc.nextLine();
                
                //Laskee teoksen id:n, joka on nykyisten teosten määrä + 1
                Statement idLaskin = con.createStatement();
                ResultSet idt = idLaskin.executeQuery("SELECT COUNT(*) AS lkm FROM teos");
                idt.next();
                id = 1 + idt.getInt("lkm");

                stmt.executeUpdate("INSERT INTO teos VALUES ('" + id + "','" + isbn + "','" + knimi + "','" + tekija + "','" + vuosi + "','" + tyyppi + "','" + luokka + "')");
                System.out.println("Uusi teos lisätty tietokantaan\n");
            }
            else {
              rs.next();
              id = rs.getInt("teos_id");
            }
            System.out.println("Haluatko lisätä teokselle niteen?\n1 = kyllä\n2 = ei");
            String uusiNide = sc.nextLine();
            while(!uusiNide.equals("1") && !uusiNide.equals("2")) {
              System.out.println("Virheellinen syöte!");
              uusiNide = sc.nextLine();
            }
            if(uusiNide.equals("1")) {
              lisaaNide(id, tietokanta);
            }
            
        } catch (SQLException e) {
            System.out.println("Virhe! " + e.getMessage());
        }
        System.out.println("Haluatko lisätä toisen teoksen?");
        System.out.println("1 = kyllä");
        System.out.println("2 = ei");
        String syote = sc.nextLine();
        while(!syote.equals("1") && !syote.equals("2")) {
            System.out.println("Virheellinen syöte!");
            syote = sc.nextLine();
        }
        if (syote.equals('1')) {
            toista = true;
        }
        else if (syote.equals('2')){
            toista = false;
        }
    }
    while (toista);
    System.out.println("Palataan etusivulle\n");
    Statement stmt = con.createStatement();
    stmt.execute("SET SEARCH_PATH TO keskusdivari");

  }

  // Lisätään nide tietokantaan vain jos sillä on jo lisätty teos, parametrinä teos_id.
  public static void lisaaNide(int teos_id, String tietokanta) {
      int nide_id;
      String hinta;
      String osto;
      String massa;
      Statement stmt;
      boolean toista;

      //Lisätään nide, kun teos on tietokannassa jo. Pyydetään lisätietoja.
      System.out.println("Teos tietokannassa, lisätään nide.");
      System.out.println("Anna myynti hinta:");
      hinta = sc.nextLine();
      System.out.println("Anna osto hinta:");
      osto = sc.nextLine();
      System.out.println("Anna kirjan massa:");
      massa = sc.nextLine();
      try {
          stmt = con.createStatement();
          do {
              ResultSet rs = stmt.executeQuery("SELECT COUNT(*) AS lkm FROM nide");
              rs.next();
              nide_id = rs.getInt("lkm") + 1;

              if(d1_oikeudet && tietokanta.equals(KESKUS)) {
                stmt.executeUpdate("INSERT INTO nide (nide_id,hinta,massa,myynti_pvm,sisaanosto,divari_nimi,teos_id) VALUES ('" + nide_id + "','" + hinta + "','"+ massa +"', " + null + ", '" + osto + "','"+ "D1" +"','"+teos_id +"')");
              }
              else if(d1_oikeudet && tietokanta.equals(D1)) {
                stmt.executeUpdate("INSERT INTO nide (nide_id,hinta,massa,myynti_pvm,sisaanosto,teos_id) VALUES ('" + nide_id + "','" + hinta + "','"+ massa +"', " + null + ", '" + osto + "','"+ teos_id +"')");
              }
              else
                stmt.executeUpdate("INSERT INTO nide (nide_id,hinta,massa,myynti_pvm,sisaanosto,divari_nimi,teos_id) VALUES ('" + nide_id + "','" + hinta + "','"+ massa +"', " + null + ", '" + osto + "','"+ "D2" +"','"+teos_id +"')");
              System.out.println("Nide lisätty!");
              System.out.println("Haluatko lisätä toisen samanlaisen niteen? 1 = kyllä, 2 = ei");
              String uusi = sc.nextLine();
              if(uusi.equals("1")) {
                toista = true;
              }
              else {
                toista = false;
              }
          }
          while (toista);
      }
      catch (SQLException e) {
          System.out.println("Virhe!: " + e.getMessage());
      }
  }
  
  // Käyttäjä hakee teosta
  public static void haeTeos() {
	  
	  Scanner sc = new Scanner(System.in);
	  int valinta;
	  String nimi;
	  String tekija;
	  String tyyppi;
	  String luokka;
	  
	  // Kysytää käyttäjältä millä hakutermillä hän teosta haluaa hakea.
	  System.out.println("Millä haluat hakea teosta? 1. Teoksen nimellä 2. Teoksen tekijällä 3. Teoksen tyypillä 4. Teoksen luokalla");
	  valinta = sc.nextInt();
	  
	  boolean oikeaValinta = false;
	  do {
	  
		  if (valinta == 1) {
			  oikeaValinta = true;
			  System.out.println("Anna teoksen nimi:");
			  nimi = sc.nextLine();
		  
			  Statement stmt;
		  
			  try {
				  // Tehdään kysely ja katsotaan, löytyykö hakusanalla teosta.
				  stmt = con.createStatement();
				  PreparedStatement prstmt = con.prepareStatement("SELECT * FROM teos WHERE nimi LIKE ?");
				  prstmt.setString(1, "'%" + nimi + "%'");
			  
				  ResultSet rs = prstmt.executeQuery();
				  System.out.println("Tulokset:");
				  while(rs.next()) {
					  System.out.println(rs.getString("nimi") + ", " + rs.getString("tekija") + ", " + rs.getString("isbn")
					  						+ rs.getString("tyyppi") + ", " + rs.getString("luokka"));
					  tilaaTeos(rs.getString("nimi"));
				  
				  }	  
			  	}		
			  
			  catch (SQLException e) {
				  System.out.println("Virhe!" + e.getMessage());
			  
			  }
		 }
	  
		  if (valinta == 2) {
			  oikeaValinta = true;
			  System.out.println("Anna tekijän nimi:");
			  tekija = sc.nextLine();
		  
			  Statement stmt;
		  
			  try {
				  // Tehdään kysely ja katsotaan, löytyykö hakusanalla teosta.
				  stmt = con.createStatement();
				  PreparedStatement prstmt = con.prepareStatement("SELECT * FROM teos WHERE tekij� LIKE ?");
				  prstmt.setString(1, "'%" + tekija + "%'");
			  
				  ResultSet rs = prstmt.executeQuery();
				  System.out.println("Tulokset:");
				  while(rs.next()) {
					  System.out.println(rs.getString("nimi") + ", " + rs.getString("tekija") + ", " + rs.getString("isbn")
					  						+ rs.getString("tyyppi") + ", " + rs.getString("luokka"));
					  tilaaTeos(rs.getString("nimi"));
				  
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
				  // Tehdään kysely ja katsotaan, löytyykö hakusanalla teosta.
				  stmt = con.createStatement();
				  PreparedStatement prstmt = con.prepareStatement("SELECT * FROM teos WHERE tyyppi LIKE ?");
				  prstmt.setString(1, "'%" + tyyppi + "%'");
			  
				  ResultSet rs = prstmt.executeQuery();
				  System.out.println("Tulokset:");
				  while(rs.next()) {
					  System.out.println(rs.getString("nimi") + ", " + rs.getString("tekija") + ", " + rs.getString("isbn")
					  						+ rs.getString("tyyppi") + ", " + rs.getString("luokka"));
					  tilaaTeos(rs.getString("nimi"));
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
		  		// Tehdään kysely ja katsotaan, löytyykö hakusanalla teosta.
		  		stmt = con.createStatement();
		  		PreparedStatement prstmt = con.prepareStatement("SELECT * FROM teos WHERE luokka LIKE ?");
		  		prstmt.setString(1, "'%" + luokka + "%'");
			  
		  		ResultSet rs = prstmt.executeQuery();
		  		System.out.println("Tulokset:");
		  		while(rs.next()) {
				  System.out.println(rs.getString("nimi") + ", " + rs.getString("tekija") + ", " + rs.getString("isbn")
				  						+ rs.getString("tyyppi") + ", " + rs.getString("luokka"));
				  tilaaTeos(rs.getString("nimi"));
				  
		  		}	  
		  	}
		  	catch (SQLException e) {
		  		System.out.println("Virhe!" + e.getMessage());
			  
		  	}
		  }
	  
	  	}
	  
	  while (!oikeaValinta);
	 
	  
  }
  
 //K�ytt�j� tilaa haetun tilauksen.
 public static void tilaaTeos(String teoksenNimi) {

	  Scanner sc = new Scanner(System.in);
	  //Kysyt��n k�ytt�j�lt� haluaako h�n tilauksen kyseisest� kirjasta tehd�.
	  System.out.println("Haluatko tilata t�m�n teoksen? [1]. Kyll� [2]. En");
	  String valinta = sc.nextLine();
	  
	  while (!valinta.equals("1") && (!valinta.equals("2"))) {
		  
		  System.out.println("Virheellinen sy�te!");
		  valinta = sc.nextLine();
	  }
	  
	  if (valinta.equals('1')) {
		  
		  // Haetaan teoksen tiedot tilauksen tekemist� varten.
		  
		  Statement stmt;
		  
		  try {
			  
			  stmt = con.createStatement();
			  
			  // Tilaus-id on nykyiset tilaukset + 1.
			  PreparedStatement tilaukset = con.prepareStatement("COUNT(*) as lkm FROM tilaus");
			  ResultSet rs = tilaukset.executeQuery();
			  int tilaus_id = rs.getInt("lkm");
			  
			  // Haetaan asiakas_id kysym�ll� k�ytt�j�nime� (Onko parempi tapa saada asiakas_id t�ss�?)
			  System.out.println("Anna k�ytt�j�tunnus:");
			  String knimi = sc.nextLine();
			  
			  
			  ResultSet asiakas = stmt.executeQuery(("SELECT asiakas_id FROM asiakas WHERE ktunnus ='" + knimi + "'"));
			  String asiakas_id = asiakas.getString("asiakas_id");
			  
			  // T�m�nhetkinen p�iv�m��r�.
			  Date sqlDate = new java.sql.Date(Calendar.getInstance().getTime().getTime());
			  
			  // Haetaan kirjan hinta ja paino.
			  ResultSet teoksenHinta = stmt.executeQuery("SELECT hinta FROM nide, teos WHERE teos.nimi = '" + teoksenNimi + "' AND teos.teos_id=nide.teos_id");
			  
			  String hinta = teoksenHinta.getString("hinta");
			  
			  // Tilauksen tila.
			  
			  String tila = "Varattu";
			  
			  // Lis�t��n uusi rivi tilaus-tauluun.
			  stmt.executeUpdate("INSERT INTO Tilaus VALUES ('" + tilaus_id + "','" + asiakas_id + "','" + sqlDate + "','" + hinta + "','" + tila +"')");
			  
			  System.out.println("Kirjan hinta on " + hinta);
			  
			  System.out.println("Vahvistetaanko tilaus?[1]Kyll� [2]Ei");
			  
			  String vahvNumero = sc.nextLine();
			  
			  while (!vahvNumero.equals("1") && (!vahvNumero.equals("2"))) {
				  
				  System.out.println("Virheellinen sy�te!");
				  valinta = sc.nextLine();
			  }
			  
			  if (vahvNumero.equals("1")) {
				  
				  PreparedStatement paivitaTila = con.prepareStatement("UPDATE Tilaus SET tila = ?" + "WHERE tilaus_id = ?");
				  paivitaTila.setString(1, "Tilattu");
				  paivitaTila.setInt(2, tilaus_id);
				  paivitaTila.executeUpdate();
				  System.out.println("Tilaus vahvistettu.");
			  }
			  
			  else if (vahvNumero.equals("2")) {
				  
				  // Jos k�ytt�j� haluaakin perua tilauksen, poistetaan tilaus.
				  
				  PreparedStatement poistaTilaus = con.prepareStatement("DELETE FROM Tilaus WHERE = ?");
				  poistaTilaus.setString(1,asiakas_id);
				  
				  System.out.println("Tilaus peruutettu.");
			  }
			  
		  }
		  
		  catch (SQLException e) {
			  
			  System.out.println("Virhe!");
			  
		  }
		  
	  }
	  
	  else if (valinta.equals('2')) {
		  
		  System.out.println("Tilausta ei tehty.");
		  
	  }
		  
 }
  
  
}
