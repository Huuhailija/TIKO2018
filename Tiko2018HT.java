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
  
  private static final String PROTOKOLLA = "";
  private static final String PALVELIN = "";
  private static final int PORTTI = 5432;
  private static final String TIETOKANTA = "";
  private static final String KAYTTAJA = "";
  private static final String SALASANA = "";
  
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

      Scanner sc = new Scanner(System.in);
      String syote = sc.next();
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
      con=DriverManager.getConnection(PROTOKOLLA + "//" + PALVELIN + ":" + PORTTI + "/" + TIETOKANTA, KAYTTAJA, SALASANA); 
    }
    catch (SQLException e) {
      System.out.println("Tietokantayhteyden avaus ei onnistu");
      System.out.println(e.getMessage());
    }
  }
}
