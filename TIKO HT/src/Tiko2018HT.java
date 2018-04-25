/*
    Tietokantaohjelmoinnin harjoitustyö 2018
    by
    Teemu Soini
    Jere Huumonen
    Joona Hautalahti
 */

import java.sql.*;
import java.util.LinkedList;
import java.util.Scanner;

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
  

  private static final String KESKUS = "Keskusdivari";
  private static final String D1 = "Divari D1";
  
  //Sisäänkirjautunut käyttäjä
  private static String kayttaja = "";
  private static boolean onYllapitaja = false;
  private static boolean keskus_oikeudet = false;
  private static boolean d1_oikeudet = false;
  
  private static int tilausId = 0;
  
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
    System.out.println("\nTervetuloa sisään " + kayttaja);
    if(!onYllapitaja) {
      System.out.println("Kayttäjäroolisi on: Asiakas\n");
    }
    else {
      System.out.println("Käyttäjäroolisi on: Ylläpitaja\n");
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
      System.out.println("\nEtusivu\nValitse komento syöttämällä komentoa vastaava numero:");
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
      hakusana = hakusana.toLowerCase();

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
	  
	  String valinta;
	  String nimi;
	  String tekija;
	  String tyyppi;
	  String luokka;
	  
	  // Kysytää käyttäjältä millä hakutermillä hän teosta haluaa hakea.
	  boolean jatka = true;
	  do {
          System.out.println("Millä haluat hakea teosta? [1] Teoksen nimellä, [2] Teoksen tekijällä, [3] Teoksen tyypillä, [4] Teoksen luokalla");
          valinta = sc.nextLine();
	  
		  if (valinta.equals("1")) {
			  System.out.println("Anna teoksen nimi:");
			  nimi = sc.nextLine();
                          jatka = valitseTeos("nimi", nimi);

		 }
	  
                  else if (valinta.equals("2")) {
			  System.out.println("Anna tekijän nimi:");
			  tekija = sc.nextLine();       
                          jatka = valitseTeos("tekija",tekija);
		  }
	  
                  else if (valinta.equals("3")) {
			  System.out.println("Anna teoksen tyyppi:");
			  tyyppi = sc.nextLine();
                          jatka = valitseTeos("tyyppi", tyyppi);
		  }
		  
                  else if (valinta.equals("4")) {
			System.out.println("Anna teoksen luokka:");
		  	luokka = sc.nextLine();
                        jatka = valitseTeos("luokka", luokka);
		  

		  }
                  else {
                    System.out.println("Virheellinen komento!");
                  }
	  
	  	}
	  
	  while (jatka);  
  }
  
  public static boolean valitseTeos(String hakukohde, String komento) {
    Statement stmt;
    boolean jatka = true;

    try {
              // Tehdään kysely ja katsotaan, löytyykö hakusanalla teosta.
              stmt = con.createStatement();
              PreparedStatement prstmt = con.prepareStatement("SELECT * FROM teos WHERE " + hakukohde + " LIKE ?");
              prstmt.setString(1, "%" + komento + "%");

              ResultSet rs = prstmt.executeQuery();
              System.out.println("Haulla saatiin seuraavat tulokset:");
              System.out.println();

              //Säilöö teosten teos_id tunnukset, listasta voidaan valita indeksiä vastaava id
              LinkedList<Integer> idLista = new LinkedList<>();
              int i = 1;
              //Tulostetaan kaikki teokset jotka hakusanalla löytyy
              while(rs.next()) {
                      idLista.add(rs.getInt("teos_id"));
                      System.out.println("[" + i +"] " + rs.getString("nimi") + ", " + rs.getString("tekija") + ", " + rs.getString("isbn") + ", " +
                                                                    rs.getString("tyyppi") + ", " + rs.getString("luokka"));
                      i++;
              }
              if(i > 0) {
                //Valitaan teos ja jatketaan tilausta, varmistetaan tilaus tai perutaan tilaus.
                System.out.println();
                System.out.println("Jatkaaksesi tilausta valitse teosta vastaava numero:");
                System.out.println("Siirry tilauksen varmistukseen painamalla [0]:");
                System.out.println("Peruuta tilaus painamalla mitä tahansa muuta näppäintä:");
                String teosValinta = sc.nextLine();
                if(teosValinta.matches("[0-9]+") && !teosValinta.equals("0") && Integer.parseInt(teosValinta) <= i) {
                  jatka = false;
                  tilaaTeos(idLista.get(Integer.parseInt(teosValinta)-1));
                }
                else if(teosValinta.equals("0")) {
                  vahvistaTilaus();
                  jatka = false;
                }
                else {
                  jatka = false;
                  peruTilaus();
                }
              }
              else
                System.out.println("Yhtäkään teosta ei löydynyt");	  
    }
    catch (SQLException e) {
            System.out.println("Virhe!" + e.getMessage());
    }
    return jatka;
  }
  
 //Käyttäjä tilaa haetun tilauksen.
  public static void tilaaTeos(int teoksenId) throws SQLException {
           
    int nideId = 0;

      //Haetaan tilausta vastaava teos ja katsotaan onko siinä vapaita niteitä
      PreparedStatement teosHaku = con.prepareStatement("SELECT * FROM teos WHERE teos_id = " + teoksenId);
      ResultSet teokset = teosHaku.executeQuery();
      teokset.next();
      System.out.println("\nValittu teos on: " + teokset.getString("nimi") + ", " + teokset.getString("tekija"));
      PreparedStatement nideHaku = con.prepareStatement("SELECT * FROM nide WHERE teos_id = " + teoksenId + " AND myynti_pvm IS NULL");
      ResultSet niteet = nideHaku.executeQuery();
      if(niteet.next()) {
        System.out.println("Teoksella on vapaita niteitä");
        nideId = niteet.getInt("nide_id");
      }
      else
        System.out.println("Valitettavasti teos on loppuunmyyty");


     if(nideId != 0) {
       //Kysytään käyttäjältä haluaako hän tilauksen kyseisestä kirjasta tehdä.
       System.out.println("Haluatko tilata tämän teoksen? [1] Kyllä, [2] En");
       String valinta = sc.nextLine();

       while (!valinta.equals("1") && (!valinta.equals("2"))) {

               System.out.println("Virheellinen syöte!");
               valinta = sc.nextLine();
       }

       if (valinta.equals("1")) {

         // Haetaan teoksen tiedot tilauksen tekemistä varten.

         Statement stmt;
         stmt = con.createStatement();

         boolean samaTilaus = false;
         
         if(tilausId == 0) {
         // Tilaus-id on nykyiset tilaukset + 1.
         PreparedStatement tilaukset = con.prepareStatement("SELECT COUNT(*) as lkm FROM tilaus");
         ResultSet tilaus = tilaukset.executeQuery();
         tilaus.next();
         tilausId = tilaus.getInt("lkm") + 1;
         }
         else {
           samaTilaus = true;        
         }

         ResultSet asiakas = stmt.executeQuery(("SELECT asiakas_id FROM asiakas WHERE ktunnus ='" + kayttaja + "'"));
         asiakas.next();
         String asiakas_id = asiakas.getString("asiakas_id");

         // Tämänhetkinen päivämäärä.
         java.sql.Date date = new java.sql.Date(new java.util.Date().getTime());

         // Haetaan kirjan hinta ja paino.
         ResultSet teosTiedot = stmt.executeQuery("SELECT hinta, massa FROM nide WHERE teos_id = " + teoksenId);
         teosTiedot.next();
         
         String paino = teosTiedot.getString("massa");
         String hinta = teosTiedot.getString("hinta");

         // Tilauksen tila.

         String tila = "Varattu";

         // Lisätään uusi rivi tilaus-tauluun jos tilauksessa ei aiempia niteitä, jos on aiempia niteitä, niin päivitetään vain hinta
         if(samaTilaus) {
           stmt.executeUpdate("UPDATE tilaus SET hinta = hinta + " + hinta);
         }
         else
           stmt.executeUpdate("INSERT INTO tilaus VALUES ('" + tilausId + "','" + asiakas_id + "','" + date + "','" + hinta + "','" + tila +"')");
         stmt.executeUpdate("INSERT INTO sisaltaa VALUES('" + tilausId + "','" + nideId + "')");
         
         //Lisätään uusi rivi "liittyy" -tauluun, eli uusi postimaksu
         int pmaksu_id = 6;
         PreparedStatement postimaksut = con.prepareStatement("SELECT pmaksu_id FROM postimaksu WHERE paino >= " + paino + " ORDER BY pmaksu_id ASC LIMIT 1");
         ResultSet pmaksu = postimaksut.executeQuery();
         if(pmaksu.next()) {
           pmaksu_id = pmaksu.getInt("pmaksu_id");
         }
         
         //Lisätään uusi rivi liittyy tauluu, joka kertoo mitä niteitä tilaukseen kuuluu
         PreparedStatement haeLkm = con.prepareStatement("SELECT COUNT(*) AS lkm FROM liittyy");
         ResultSet lkm = haeLkm.executeQuery();
         lkm.next();
         int liittyy_id = lkm.getInt("lkm");
         stmt.executeUpdate("INSERT INTO liittyy VALUES('" + tilausId + "','" + pmaksu_id + "','" + liittyy_id + "')");
         PreparedStatement paivitaTila = con.prepareStatement("UPDATE nide SET myynti_pvm = '" + date + "' WHERE nide_id = " + nideId);
         paivitaTila.executeUpdate();
         
         //Haluaako käyttä lisätä toisen niteen tilaukseen
         System.out.println("\nTeos lisätty tilaukseen.\n[1] Lisää toinen teos tilaukseen, [2] Vahvista tilaus");
         String toinenTilaus = sc.nextLine();
         while(!toinenTilaus.equals("1") && !toinenTilaus.equals("2")) {
            System.out.println("Virheellinen syöte!");
            toinenTilaus = sc.nextLine();
         }
         if(toinenTilaus.equals("1")) {
           haeTeos();
         }
         else {
           vahvistaTilaus();
         }
       }
       else if (valinta.equals("2")) {
         System.out.println("Palataan hakuun...");
         haeTeos();

       }  
     }
     else {
       System.out.println("Palataan hakuun...");
       haeTeos();
     }
  }
  
  //Peruu tilauksen. Poistaa kaiken tilaukseen liittyvän tietokannasta.
  private static void peruTilaus() throws SQLException {
    Statement stmt = con.createStatement();
    PreparedStatement haeNiteet = con.prepareStatement("SELECT nide.nide_id FROM nide, sisaltaa, tilaus WHERE tilaus.tilaus_id = " + tilausId +
                                                       " AND tilaus.tilaus_id = sisaltaa.tilaus_id AND sisaltaa.nide_id = nide.nide_id");
    ResultSet niteet = haeNiteet.executeQuery();
    while(niteet.next()) {
      stmt.executeUpdate("UPDATE nide SET myynti_pvm = NULL WHERE nide_id = " + niteet.getInt("nide_id"));
    }
    
    stmt.executeUpdate("DELETE FROM liittyy WHERE tilaus_id = " + tilausId);
    stmt.executeUpdate("DELETE FROM sisaltaa WHERE tilaus_id = " + tilausId);
    stmt.executeUpdate("DELETE FROM tilaus WHERE tilaus_id = " + tilausId);

  }
  
   private static void vahvistaTilaus() throws SQLException {
     
     //katsotaan onko tilausIdtä olemassa, eli onko se jotain muuta kuin alkuarvo
     if(tilausId != 0) {
       
     //Lasketaan hinnat ja postikulut ja tulostetaan ne käyttäjälle
      PreparedStatement hintaHaku = con.prepareStatement("SELECT hinta FROM tilaus WHERE tilaus_id = " + tilausId);
      ResultSet hinnat = hintaHaku.executeQuery();
      hinnat.next();
      double hinta = hinnat.getDouble("hinta");

      double postikulut = 0;
      PreparedStatement pmaksuHaku = con.prepareStatement("SELECT postimaksu.hinta FROM postimaksu, tilaus, liittyy " +
                                       "WHERE tilaus.tilaus_id = " + tilausId + " AND tilaus.tilaus_id = liittyy.tilaus_id AND " +
                                       "liittyy.pmaksu_id = postimaksu.pmaksu_id");
      ResultSet pmaksut = pmaksuHaku.executeQuery();
      while(pmaksut.next()) {
        postikulut += pmaksut.getDouble("hinta");
      }

      System.out.println("\nTilauksen hinta = " + hinta + " ja toimituskulut = " + postikulut);

      //Vahvistetaanko vai perutaanko tilaus
      System.out.println("Vahvistetaanko tilaus? [1] Kyllä, [2] Ei");

      String vahvNumero = sc.nextLine();

      while (!vahvNumero.equals("1") && (!vahvNumero.equals("2"))) {

        System.out.println("Virheellinen syöte!");
        vahvNumero = sc.nextLine();
      }

      //Vahvistus
      if (vahvNumero.equals("1")) {
        PreparedStatement paivitaTila = con.prepareStatement("UPDATE tilaus SET tila = ?" + "WHERE tilaus_id = ?");
        paivitaTila.setString(1, "Tilattu");
        paivitaTila.setInt(2, tilausId);
        paivitaTila.executeUpdate();

        System.out.println("Tilaus vahvistettu.");
        tilausId = 0;
      }

      //Peruminen
      else if (vahvNumero.equals("2")) {
        peruTilaus();
        tilausId = 0;
        System.out.println("Tilaus peruutettu.");
      }
      
    }
     else {
       System.out.println("Tilausta ei ole olemassa.\nSiirrytään takaisin etusivulle");
     }
  }
   
 }

