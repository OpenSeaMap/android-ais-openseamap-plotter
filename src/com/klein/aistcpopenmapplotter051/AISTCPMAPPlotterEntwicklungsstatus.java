package com.klein.aistcpopenmapplotter051;



/**
 * @author vk1
 * Entwicklungsstatus 11.9.11
 * Der Test in Waarns hat in wesentlichen Teilen geklappt.
 * 
 * Probleme: Aus der Datenbank wird nicht richtig gelöscht
 * es treten Duplikate von Targets auf
 * 
 * Derr Parser trägt richtig in die Datenbank ein
 * TargetList.deleteOldtargets 
 * hier wird zwar aus der Liste, nicht aber aus der Datenbank gelöscht
 * TagetList.deactivateOldTargets
 * hier wird in der Liste deaktiviert, 
 * die Änderung aber nicht in der Datenbank upgedated
 * hier standen jeweils Verweise auf die Activity
 * müsste durch Service bzw Parser ersetzt werden
 * 
 * Das Problem ist Context.
 * Was bedeutet Context in diesem Zusammenhang, 
 * das habe ich noch nicht verstanden. Ist das die Applikation?
 * 
 * Fehler:
 * Duplikate in der Datenbank  fixed
 * Manchmal werden PositionReports (Klitfrak) mit völlig falschen Daten
 * zu Position un SOG eingetragen ? Warum, das hatte ich früher
 * schon mal, hier stimmt irgend ein Index nicht fixed
 * ein Target überträgt falsche Daten, LAT 181, lon 91 , sog 102
 * 
 * Es gibt mehr Nachrichtentypen als 123 und 5, fixed
 * Siehe Target aus AISMapLotter
 * 
 * Änderungen:
 * 
 * targetList wird mit Context initialisiert um einen
 * eigenen DB Adapter realisieren zu können fixed
 * 
 * todo:
 * Weitere Messagetypen einbauen, fixed
 * Duplikate in der datenbank? fixed
 * Menupunkt reset Datenbank zum kompletten Löschen aller Einträge??
 * nach einen neustart werden die Zeiten alle mit 00:00:00 angezeigt fixed
 * 
 * 4.10.11
 * Bei einer Installation auf einem neuen Gerät wird nicht geprüft,
 *  ob Bluetooth eingeschaltet ist. Die BT Adresse ist leer. 
 *  Man muss erst in Einstellungen pairen und dann das BT_modul auf der Startseite auswählen.
 *  Hier fehlt ein Hinweis an den Nutzer.
 *  
 *  5.10.11
 *  AISTCPMAPPlotter011 erzeugt 
 *  teilweise umbenannt
 *  der BT Service muss noch gegen den TCP-Service ausgetauscht werden
 *  vermutlich fehlen Initalisierungen 
 *  Wichtig: Namen der Activities im Manifest prüfen und ändern 
 *  
 *  letzter Stand: 5.10.11 18:00 Uhr
 *  erfolgreich getestet mit Dell Mini als Server mit aisdecoder 1.03 auf D: und daten
 *  aus Warns. Angemeldet als vk1.  Internetverbindung über Festnetz und Router
 *  Verbindung über Wlan sip11 , sowohl Handy als auch dell mini
 *  IP-Adresse 172.16.200.102 für Server
 *  Referenzen auf BT Funktionalität entfernt bzw auskommentiert
 *  Zu prüfen ist noch der Broadcastreceiver in TCPNetzwerkService
 *  soll der Edimax verwendet werden , muss die ip angepasst werden
 *  in Einstellungen
 *  es fehlt noch die Bereitstellung der AIs-Daten vom Decoder über den
 *  arduino mit ethernet
 *  getestet mit ardiuno22-->Bespiele->rs232Telnetserver090 
 *  arduino muss einmal nach einschalten resetted werden, dann ansprechbar 172.16.200.177 9999
 * beachte jeweils map key für Entwicklungsrechner anpassen!!
 * 6.10.11
 *<!-- beachte je nach Entwicklungsrechner!!! in mapview.xml map key anpassen
 *      map key mac mini 0GtjrwcNbz0ZYXFe5ArA2Neo1UYDm6CWta6OGeQ
 *     map key dell netbook  0GtjrwcNbz0bGtGSCpODRF9qqEH1DExDgBeIqZQ 
 *     -->
 *     eigenes Schiff wird als Symbol angezeigt, hat aber bisher Kurs un SOG 0
 *     es wird nur die Position upgedatetd
 *     inaktive Targets sind durchgestrichen
 *     21.10.11 nochmaliger test, arduino 092 sendet jetzt no-AIS-data wenn keine Daten vorhanden sind
 *     das wird auch im Logger angezeigt
 *   26.10.11 Fehler in der Methode AISTCPMapPlotter.setPositionAndUTCTime  bei der
 *   Wandlung von Koordinaten beseitigt
 *   Speed und bearing werden aus GPS übernommn und myShip entsprechend aktualisiert
 *   
 *   16.11.2011
 *   Version 012 für Tablet Archos
 *   hierbei muss alles für BT und GPS entfernt werden
 *   die Koordinaten werden über TCP angeliefert und als RMC Message 
 *   im Parser ausgewertet. 
 *   Das Problem ist, wie die Daten in den Plotter kommen.
 *   Wenn der Service gestartet ist, gibt es den Plotter noch nicht
 *   das muss so sein, da sonst das Problem auftritt, dass der Plotter auf die
 *   Targetlist zugreift, obwohl der Service die grade erst aufbaut.
 *   
 *   Problem: dabei ist keine Richtungsauswertung möglich,
 *   jedenfalls nicht so einfach, 
 *   20.11.2011
 *   Lösung für GPS- daten im Plotter
 *   die GPS Daten werden vom Parser mittels Broadcast an den mapplotter gelierfert.
 *   genial einfach, wenn man weiss wie es geht
 *   Verwendet : Broadcast receiver, intent filter, intents , daten mit putextra !!! 
 *   jetzt kann man im Plotter auswählen, ob die GPS-daten per internem GPS  oder 
 *   per NMEA angeliefert werden!!!!! 
 *   
 *   30.11.11
 *   Auf dem Archos Tablet gibt es bei vielen Daten häufig Abstürze
 *   Auf dem Dell-Sreak mit 2 kern gabz andere Performance auch bei hoher Last.
 *   1.12.11
 *   
 *   Auswahl der gps_quelle über Einstellungen 
 *   0 kein gps , 1 intern, 2 extern per nmea
 *   bei 1 wird GpsLocationServiceLocal gestartet
 *   bei 2 wir der Broadcastreceiver in onStart von AISTCPPlotter registriert
 *   
 *   12_01_11
 *   Zentrieren der Karte auf ein aus der Liste ausgewähltes Target
 *   Wesentliche Änderungen bei der Rückgabe von resultCodes und Daten
 *   Siehe dazu TargetEdit-->onCreate  showOnMapButton 
 *   AISListActivity --> showTargetOnMap und onActivityResult
 *   AISMapPlotter--> onActivityResult und centerMapToSeletedTarget
 *   
 *   Auf der Startseite Warnhinweis und Einschränkungen definiert
 *   
 *   12_02_28
 *   die Version 12 läuft auch auf dem Archos
 *   Version 013 und 014 habeb Datenfehler in der Source auf Macmini --> gelöscht
 *   neue Version 015 auf MacMini abgeleitet aus Version 012 auf Toshiba
 *   da der Packagename geändert wurde musste das Manifest angepasst werden
 *   beachte auch die Verwendung des packagenames in AISPlotterGlobals
 *   logging für ship-report korrigiert
 *   
 *   12_02_29
 *   
 *   ProgressDialog für das Laden der Targets aus der Datenbank, spinning geht
 *   Progressbar horizontal wird nicht aktalisiert
 *   Hilfe aktualisiert für Openmap
 *   
 *   experiment, mit layout aus resource  button test unsichtbar bei klick, layout müsste geändert werden
 *   
 *   12_03_21
 *   WayOverlay eingefügt
 *   im onTap Dialog kann jetzt das Zeichnen eines tracks ausgelöst werden
 *   Die Tracks werden nicht gespeichert, wird die activity verlassen so verschwinden Tracks und 
 *   die Targets müssen neu ausgewählt werden
 *   Die app speichert nicht den Status der Tracks,
 *   dir Tracks werden lediglich in updateTargetOnOverlay if Item is found and hasTrack gezeichnet
 *   
 *   Es können verschiedene Kartentypen ausgewählt werden , Mapnik, Eniro, Eniro Nautical , 
 *   Es fehlt der filePicker um eine offlinekarte auszuwählen
 *   
 *   12_03_21 20:00
 *   neue Version 030 
 *   Umstellung auf snapshot 0.3.1
 *   File Picker eingebaut
 *   Kartenauswahl auf Startpage oder dynamisch im Plotter, funktioniert nur für NL und nrw, 
 *   man muss einen Startpunkt auf der Karte  und eine hohe Zoomstufe vorgeben, sonst hängt der Renderer
 *   Tracks werden noch nicht gespeichert
 *   
 *   12_04_03 Vorbereitungen für das Speichern des Tracks in der DB
 *  WEsentliche Änderung:
 *  AIStarget hat neues Attribut mHasTrack
 *  wird in der db gesichert und auch wieder zurückgelesen
 *  es fehlt noch das Eintragen der Spur aus der db nach einem Neustart der app
 *  muss in fillTargetListWithData() in nmeaparser organisiert werden
 *  
 *  12_0404
 *  Datenformat für lat/lon in db ist unterschiedlich 
 *  in tracktable : in grad 52.23 / 6.34
 *  in targets : lesbare form 52' 12,67 N, 6' 34,8 E
 *  Fehlerträchtig, sollte geändert werden
 *  
 *  Tracks aufzeichen läuft, nach neustart der app werden die gespeicherten Tracks richtig angezeigt
 *  das Löschen der Tracks, wenn die targets gelöscht werden ist noch nicht getestet scheint aber ok zu sein nach Neustart des plotters
 *  Eine einzelen Spur kann nicht gelöscht werden, da die Strecken keinem Target im Overlay zugeordnet sind
 *  Status hasTrack wird in edittarget angezeigt, eine Änderung wird aber nicht aktualisiert 
 *  
 *  12_04_19
 *  neu: Tracks können als Liste angezeigt und als Text angezeigt werden.
 *  Tracks können gespeichert (als gpx)und gelöscht werden im festen <Verzeichnis /sdcard2/trackdata
 *  Bei gelöschten Tracks sind noch nicht alle Referenzen geklärt, 
 *  die Tracks bleiben bis zum nächsten Start der Activity aber sichtbar, bisher kein Löschen möglich,
 *  da die Trackelemente nicht einem Track zugeordent werden können
 *  
 *  12_05_16 
 *  Erstellen einer Route ist möglich,
 *  neu, editieren, speichern, laden Verzeichnis /sdcard2/trackdata
 *  Route wird als default Route abgelegt (Route_0000_Datum.gpx)
 *  
 *  12_06_01
 *  
 *  Routen können mit Namen abgelegt werden, dazu erschein ein Dateinamendialog
 *  Wegepunkte können auf der Route angezeigt oder versteckt werden
 *  Symbole im Plottermenu eingebaut
 *  wen ein menu.xml geändert wird , gibt es Fehler bei der Zuordnung
 *  Abhilfe: Project-->Clean
 *  in firststart wird mapnik als erster Kartentyp ausgewählt
 *  
 *  12_06_13
 *  
 *  Fehler: ändert sich die COG eines Targets, aber nicht die Position, so wird erfolgt kein GUI-Update
 *  korrigert
 *  Fehler: bei onTap auf myShip wir die Zeit des letzten updates nicht mitgeführt
 *  korrigiert für nmeaGPS und internes GPS
 *  
 *  12_06_17
 *  trace einer Route implementiert und grob getestet
 *  dabei wird mit dem gewählten Maßstab jeder Routenpunkt als Mitte der Karte nacheinander 
 *  zu Zentrum der Karte gemacht. Dabei wird der Kartencache gefüllt
 *  Problem : Bei höher Auflösung ( z.B. 200m) und weit auseinanderliegenden Routenpunkten werden 
 *  nicht alle Tiles geladen
 *  todo: Liste von Tracepunkten erstellen, dabei kann die Funktion zum Auslesen des Maßstabs
 *  verwendet werden um Zwischenpunkte zu erzeugen
 *  
 *  12_06_18
 *  Route Textaktivity soll nun eine Liste mit Entfernumgen und Kursen anzeigen
 *  todo Umkehren einer Route
 *  
 *  12_06_30
 *  Die Eniro nautical tiles werden jetzt von Host1-4 angefordert
 *  Die Eniro map tiles werden jetzt von Host1-4 angefordert
 *  SOG für eigenes Schiff wird angezeigt
 *  
 *  12_07_14
 *  Fehler: Falsche Anzeigen bei eigenem Kurs
 *  Routenlänge wird falsch berechnet
 *  Beim Speichern und Rückladen tritt ein Fehler aus, wenn die Route korrigiert wurde,
 *  anscheinend wird der entfernte Routenpunkt nicht entfernt.
 *  
 *  12_07_25
 *  Der Tilecache kann restauriert werden. Muss noch getestet werden , ok
 *  Die Entferungen in einer Route wurden falsch berechnet, calculateDistance war nicht korrekt
 *  es wurde nicht berücksichtigt, dass die Umrechnung Grad -nautische Meile von der Breite (Latitude) abhängt
 *  
 *  12_09_05
 *  Version 031
 *   Die privaten Klassen aus AISTCPOpenMapPlotter 
 *   ArrayRoutePointsItemizedOverlay
 *   RouteOverlayItem 
 *   RouteItem
 *   AISOverlayItem
 *    sind ausgelagert worden
 *   Verweise auf getResources sind durch  context.getResources().... ersetzt worden
 *   
 *   
 *   
 *  12_10_27
 *  Version 041
 *  wichtig: Referenced Library geändert: mymapsforge030_withdependenciestest.jar
 *  diese Libray ist aus den Projekten mapsforge und mapsforge-map-reader{tags/0.30./mapsforge...] erzeugt worden
 *  TileDownloader.executeJob(..) ist nicht mehr final und kann überschrieben werden
 *  Referenzen geändert (lib-> libs), beachte dabei .classpath
 *   EniroNauticalTileDownloader.executeJob(..) überschreibt 
 *   dabei timeout eingebaut und Nachrichten in Logger über erfolgreiches laden
 *   bricht jetzt korrekt ab, wenn die Internetverbindung zwischendurch unterbrochen wird
 *   Notification wenn tile download , oder unmöglich ( in EniroNauticalTile Downloader)
 *   
 *  12_10_30
 *  OpenseamapTileDownloader kann jetzt auch die seamarks darstellen
 *  alle geladenen Tiles werden in einen eigenen cache unter OSMCachedata geschrieben
 *  
 *  CreateDirectoryIfNecessary ausgelagert in PositionTools
 *  kann jetzt auch andere moutpoints als sdcard2 benutzen
 *  
 *  Verschiede Einträge in Strings (german) ergänzt
 *  
 *  12_11_15
 *  
 *  Die Downloader schreiben jetzt alle in einen eigenen Cache
 *  
 *  Reihenfolge der verfügbaren Downloader und Renderer geändert
 *  TCPNetzwerkService.netzwerkVerbindungHerstellen(): Wird der Datenstrom vom AIS für länger als zwei Minuten unterbrochen so wirft der Service eine Notification "keine AIS Daten"
 *  
 *  signierte Version erzeugt, dabei debuggable Attribut im Manifest auf false
 *  
 *  12_11_19 Markus meldete dass die app nach Drücken des confirm-buttons gecrashed ist auf dem Medion 4.0 Tablet
 *     in StartPage.confirmButtonPressed wurde restoreCacheSerFile aufgerufen , dort wurde sdcard2 exists voruasgesetzt
 *     
 *     createExternalDirectoryIfNecessary in PositionTools ausgelagert
 *     Der Pfad zur SD-Karte wird über Environment2.getCardDirectory bestimmt
 *  Probleme bei 4.0 und folgende mit PendingIntents in showNotification, Verschiedene  Stellen im DownLoader, StartActivity
 *  getActivity() führt zu einem Absturz. Deshalb neu:  pendingIntent = null, brauchen wir eh nicht da die Activity nicht aus der Notification gestartet wird
 *  Geht leider nicht so einfach, pendingIntent=null führt in os < 14 ($.0) zu einem Absturz.
 *  Dsahalb int aVersion  = Build.VERSION.SDK_INT;
 *        PendingIntent pendingIntent = null;
 *		  if (aVersion < 14) pendingIntent = PendingIntent.getActivity(this, 0, null, 0); 
 *	Neuer build 12_11_20	19:35 Uhr  
 *	
 *13_01_11
 *
 * Weitere Tests auf Samsung Tablet 10.1, Test mit 4.0.3 ok
 * Alle online-Downloader haben jetzt ihren eigenen Cache, siehe 12_11_15, dabei wird noch nach den Zoomstufen unterschieden.
 * Verzeichnisse wurden nicht richtig erzeugt, deshalb kein Cache angelegt, korrigiert	  
 * 
 * Neuer Build  041: 03 siehe assets:hilfe.txt
 * 
 * 13_02_14
 * 
 * EniroAerial mit seamarksoverlay neu aufgenommen, Einträge in AISPlotter.onCreate() und in arrays und strings
 * Dieser Downloader legt einen eigenen Cache in AISPlotter/cachedata/EnrioAerialWithSeamarks an 
 * Die Bestimmung des Speicherpfaded geschieht über Environment2, erkennt auch die SD auf Samsung
 * 
 * andere Downloader müssen korigiert werden
 * 
 * 
 * 13_03_12
 * Version 051 erzeugt
 * SeamarksOverlay eingebaut für Openseamap offline Karte
 * Splitscreen mit Seamarksoverlay und Bing AERIAL
 * dazu Startpage erweitert und AIDPlotter über extras im INtent parametrisiert
 * 
 * 
 * 13_03_16
 * Version 051 Build 01
 * Overlays geändert , es gibt jetzt nur noch seamarks Overlay, ein WayOverlay und ein Itemized Overlay
 * aus Speichergründen wurden die verschiedenen Overlays zusammengefasst
 * die AISTargetItems und die RouteItems befinden sich nun auf MyItemizedOverlay.
 * MyItemized Overla führt eigene listen und die AISTargetItems und RouteItems unterscheiden zu können
 * Alle Wage sind nun auf MyWaysOverlay.
 * Hier gibt ein Dictionary, dass die verschiedenen Ways ( Route) Tracks zu AISTargets verwaltet.
 * Ein neuer Way wird immer mit Angabe seines Keys gespieichert
 * 
 * todo :Der SpecialManueverIndicator ist die Blaue Flagge auf dem Rhein, das Auftreten wird geloggt, aber noch nicht ausgewertet
 * 
 * 13_04_06 
 * Titelzeile geändert 
 * neuer Build 051 Build 02
 * 
 * 13_05_11
 * 
 * Das Symbols zum Target wurde nach einem Positionsupdate auf Frame gesetzt, obwohl ein Schiffs-
 * info mit Namen vorlag. Fehler in AISTCPOpenMapPlotter.updateTargetOnOverlay wenn das Target nicht neu
 * erzeugt wurde, sondern schon vorhandn war, wurde das falsche Symbol eingetragen, fixed
 * Dss info bei ontap im myItemized overlay ist um die Blaue Tafel erweitert worden
 * 
 * neuer Build ist notwendig
 * 
 * 13_07_20
 * Der Download von Eniro Nautical ist plötzlich unzuverläsig. 
 * Viele Downloads werden nach 1000 ms abgebrochen. Ist das ein Problem mit der Telenor-Karte?
 * Bisher wurde schneller geladen.
 * In EnironauticalTileDownloader.executeJob wurde der timeout auf 1000 ms gesetzt.
 * Übernehme jetzt wieder den übergebenen Wert 5000 aus dem Parameter aTimeout.
 * Das Problem ist ungelöst, scheint aber an der Verbindung zu liegenn, auch der Browser erhält häufig keine 
 * Antwort
 * 
 * 13_07_24 
 * Eine alte Route wurde nach dem Laden nicht angezeigt. Wurde die app beendet und neu gestartet, so war die Route wieder da.
 * Das Problem lag an der Methode restoreRouteFromDatabase die vom Ladethread nicht aufgerufen wurde. Es gab ein Problem mit einem Pending event,
 * siehe Kommentar in restoreRouteFromDatabase. Jetzt wird geprüft, ob die Routine grade ausgeführt wird und ein zweite Ausführung verhindert.
 * 
 * 13_09_26 Das Problem der Tracks (Laden eines gespeicherten Tracks) ist ungelöst
 *  Ein gespeicherter Track kann in google-earh angezeigtt werden (Beispiel Vitaskär von Hanö nach Nogersund)
 *  Nach einem Resume der activity sind die Spuren nicht menr sichtbar, man könne dasi der Vitaskär simulieren
 *  Fehler in TrackTextActiviy beseitigt, es werd eine Exception, mit nur eine SQL-Excepion geworfen, wenn der Name nicht definiert ist.
 *  
 *  13_09_27
 *  restoreTrackInInitphase wurde nicht ausgeführt, da der Way noch nicht im Dictionary war und Clear auf einen Nullpointer angewendet wurde.
 *  Diese Excepion wurde zwar abgefangen, aber nicht geloggt.
 *  
 *  13_10_01
 *  Die Zeit des letzen Positionsupdate wurde für myShip und internes GPS nicht richig angezeigt,
 *  Es besteht eine Differenz zwischen der Zeit aus Location und System.imeMillis(), in setPositionFromInternalGPS geändert.
 *  
 *  neuer Build notwendig
 *  
 *  13_10_13
 *  
 *  neue Tileserver wie im openseamapviewer in OpenseamapTileAnd SeamarksDownloader
 *  in der Regensburg-Linz Karte sindbei Vilshofen Flusskilometer, die werden noch nocht erkannt
 *  <node id="33259995" lat="49.0168378" lon="12.1982919" version="4" timestamp="2013-09-06T08:22:12Z" changeset="17699460" uid="644905" user="RTU">
 *   <tag k="river:waterway_distance" v="2371"/>
 *   <tag k="seamark:distance_mark:category" v="not_installed"/>
 *  <tag k="seamark:distance_mark:distance" v="2371"/>
 *   <tag k="seamark:distance_mark:units" v="kilometres"/>
 *   <tag k="seamark:type" v="distance_mark"/>
 *  </node>
 *  Es fehlen die Brückenhöhen
 *  Die sonstigen Seezeichen waren in SeamarksOverlay abgeschaltet, sei 13_10_13 wieder eingeschaltet
 *  es muss ein Menupunkt dazu eingerichtet werden, das bedeutet aber auch, das eine Liste dieser zeichen geführt werden muss
 *  siehe openseamapviewer
 *
 *  
 *  private static final String HOST_NAME = "t2.openseamap.org"
 *	private static final String HOST_NAME_SEAMARKS = "t1.openseamap.org";
 *
 * tile server für OSeaM auf osm2.wtnet.de geändert 2013_12_04
 *
 * 13_12_01
 * AISTCPOpenMapPlotter.mNMEAParserDataReceiver erweitert um nmea2000 Daten anzuzeigen
 * TCPNetzwerkService.processMessage erweitert um nmea2000-canboat messages zu verarbeiten
 * NMEAParser: neu processPGN... , processMessage dekodiert auch json,
 * muss noch genauer überarbeitet werden
 * speed water referenced, speed ground referenced, cog
 * 
 * 
 * 13_12_08 
 * Fehler: Wird eine externe GPS-Quelle verwendet und soll der Track aufgezecinet werden , so werden die 
 * Positionen nicht in die Track-Table zu 123456789 eingetragen
 * vergleiche dazu AISTCPOpenMapPlotter.setPositionFromInternal vor updateTargetOnOverlay
 * 
 * Korrigiert, nmeaBroacastReceiver Zeile 645
 * 
 * 13_12-10 
 * Auf 7 und 10 inch Bildschirm kann mit oder ohne Datenscreen gestartet werden
 * auf kleinen Displays wird auf der Startseite der Datascreen button nicht gezeigt
 * neuer build 051 06
 * 
 * 13_12_12
 * 
 * In der Analyse der nmea0183 Daten bei Lat und lon (RMC und GGA )war ein schwerer Fehler, nund S bzw W und O wurde nicht analysiert
 * Zudem wurden LAT und Lon Daten mit anderen Längen ignoriert. erledigt
 * 
 * neuer build 051 07 assets geändert
 *  
 * 13_12_17
 * 
 * neuer build für google play 052 build01
 * da playstore auf einer höheren versionsnummer besteht
 * 
 * schwerer fehler in nmeaparser.processVDMMessage
 * aus irgendwelchen Gründen wurde das Abschluss * in der Message vorherschon abgetrennt.
 * Da aber nicht mehr mit AIS-daten getestet wurde, ist dad nicht aufgefallen
 * siehe Zeile 681 ff 
 * 
 * wir mussen noch neu Bauen
 * das die Version auf dem playstore defekt ist
 * 
 * 13_12_26
 * 
 * Es gibt ein Problem mit COG und SOG bei der Simulation eines Trips
 * Enthält die Datei neben RMC Messages auch andere Messages z.B. DBT , so wird versucht
 * rechnerich COG und SOG zu bestimmen, siehe Ende von NMEAParser.setPositionOfOwnShipFromNMEA0183
 * Zeile 1485 ff
 * Dadurch springt COG und SOG hin und her.
 * Änderung: wir berechnen SOG und COG nur, wenn die letzte RMC Message älter als 5 sec ist
 * aus der RMC messgae wird COG aus Track made good entnommen
 * 
 * 
 * 13_12_29 neuer build 053 01
 * Nummer geändert in manifest, Startpage, disclaimer, hilfe,read_asset , res/values/strings, res/values-de/strings
 * 
 * 14_01_06 neuer build 053 01
 *  Testkarte_Wismarbucht in den assets hinzugefügt, kopiert nach aisplotter/Mapdata
 *  
 *  
 * 14_01_07 neue Version 054 komplett
 * alte Library stürzt ab in InMemoryTileCache
 * neue Bibliothek gebaut dabei Fehler korrigiert siehe 
 * zusätzliche Icons für die AIS-Sysmbole (144 dpi)in res/drawable-xhdpi , die anderem Sysmbole aus drawable-hdpi waren zu winzig 
 * Es verbleiben noch die Tonnen, die zu klein sind , werden durch einen Faktor 2 vergrössert wenn density =320, sie onCreate in AISPlotter
 *  Fehler, in MyItemizedOverlay.onTap war bei ownShip die referenz zur Track verloren gegangen, korrigiert
 *  
 * es muss eine neue Version auf google play gelegt werden
 * 
 * 14_01_07 neue Version 055 (wegen Google play)
 * Track bei myShip korrigiert
 * 
 * auf einem Tablet kann man keine xml files mit firefox herunterladen und speichern. da die sofort interprtiert werden.
 * Deshalb werden die xx_seamarks.xml jetzt xx_seamarks_dat genannt
 * 
 * Neue Version 056
 * liest auch seamark_dat,
 * Fehler in Seamarks osm, wenn eine colour in einer Boje nicht spezifiziert, null pointer exeption, da colourstr null
 *  Automatisches Löschen von AIs : ein, Cache ein als default 
 *  
 *  2014_01_12
 *  noch eine null-pointer-exception in SeamarksOverlay.drawSectorFires Zeile 1130 korrigiert
 *  Ellipse für Brückenhöhen berücksichtig mDisplayFactor (bei 320 dpi = 2) in SeamarksDrawable
 *  Displayfactor für 320 dpi auf 1.5 geändert
 *  
 *  2014_01_14
 *  Neue Version 057 für Store
 *  
 *  2014_01_22
 *  
 *  AIS-SART Symbol für test, AISPlotter.updateTargetonOverlay wertet navstatus aus
 *  NMEAParser.sendGUIUpdateEvent sendet navstatus
 *  App kann alle NMEA Daten loggen, einstellbar in den Einstellungen
 *  das loggen findet im TCP-Service statt
 *  
 *  2014_01_27
 *  
 *  Eine neu installierte app startet mit openseamap (online)
 *  gps-Icon (notification) bei nexus 7 nicht vorhanden, ist nicht mein problem, da daten geholt werden, siehe datenlogger
 *  snap-icon zentriert nicht als erste Aktion auf der Karte, ( Hinweis Malcolm), koorigiert
 *  Schiffsdaten auch vom internen gps soweit verfügbar
 *  Hilfe-Text ergänzt
 *  Neue Version 058 für store
 *  Backuo 058_B01
 *  
 *  2014_02_04
 *  AIS-SART eingebaut, als Test und als Alert
 *  
 * 
 *  todo:
 *  12_08_03 restoreTrackInInitPhase muss programmiert und getestet werden erledigt 13_12_08
 *  12_08_03 AISItemizedOverlay.ontap es gibt keine Möglichkeit, das trackrecording zu unterbrechen und später fortzusetzen
 *  12_08_03 putTargetsOnMap setzt Symbole nur für 
 *  AISPlotterGlobals.DISPLAYSTATUS_HAS_POSITION
 *  AISPlotterGlobals.DISPLAYSTATUS_HAS_SHIPREPORT
 *  AISPlotterGlobals.DISPLAYSTATUS_BASE_STATION
 *  andere Fälle bleiben unberücksichtigt
 *  12_08_03 route trace muss automatisch beendet werden, wenn  die activity inaktiv wird
 *  12_08_03 Alarmradius wird nicht berücksichtigt, siehe auch onStop radius wird nicht gesichert
 *  12_08_03 AISPlotterGlobals.PREV_LAST_GEOPOINT_LAT/LON hält lastMapCenter
 *  12_08_03 onResume fileSystemTile.cache.setcapacity(TILE_CACHE_MAX_CAPACITY); prüfen
 *  
 *  Die Geschwindingkeit des eigenen Schiffs wird nur bei RMC Message berechnet
 *  Der A50-Plotter liefert nur GGA, daraus keine Geschwindigkeitsberechnung möglich
 *  Umkehren einer Route
 *  für 0.30 Exportversion erstellen
 *  für Version 0.50
 *  Für jedes AIS-Target sollte  die Länge gespeichert und angezeigt werden.
 *  eine erste Karte muss beigefügt werden, oder auf mapnik in firststart stellen , fixed 
 **/
  

public class AISTCPMAPPlotterEntwicklungsstatus {

}
