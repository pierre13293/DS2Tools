package adver.sarius.ds2tools.datacollector;

import java.io.BufferedReader;
import java.io.IOException;
import java.math.BigInteger;
import java.util.Map;
import java.util.Set;

import adver.sarius.ds2tools.extended.ForschungExt;
import adver.sarius.ds2tools.extended.ShipBaubarExt;
import adver.sarius.ds2tools.extended.ShipTypeExt;
import net.driftingsouls.ds2.server.cargo.Cargo;
import net.driftingsouls.ds2.server.cargo.ItemID;
import net.driftingsouls.ds2.server.entities.Forschung;
import net.driftingsouls.ds2.server.entities.Weapon;
import net.driftingsouls.ds2.server.entities.Weapon.Flags;
import net.driftingsouls.ds2.server.ships.ShipBaubar;
import net.driftingsouls.ds2.server.ships.ShipClasses;
import net.driftingsouls.ds2.server.ships.ShipType;
import net.driftingsouls.ds2.server.ships.ShipTypeFlag;

public class SchiffInfoProcessor extends DSPageProcessor {

	// Datenblatt https://ds2.drifting-souls.net/ds?module=schiffinfo&sess=&ship=1
	
	// ShipType
	// Baukosten 
	// Waffen 
	// Modulslots
	

	/** Location of the pictures without the filename */
	private static final String PICTURE_PATH = "data/dynamicContent/";
		
	/**
	 * Get the ShipClasses enum from its name.
	 * 
	 * @param shipClassString name of the ShipClass, singular or plural.
	 * @return found ShipClass or null.
	 */
	private ShipClasses getShipClass(String shipClassString) {
		for (ShipClasses s : ShipClasses.values()) {
			if (s.getSingular().equals(shipClassString) || s.getPlural().equals(shipClassString)) {
				return s;
			}
		}
		return null;
	}
	
	/**
	 * create a new Forschung with the given id.
	 * 
	 * @param id id of the Forschung.
	 * @return empty Forschung with given id.
	 */
	private Forschung getForschung(int id) {
		return new ForschungExt(id);
	}
	
	/**
	 * Determines the race by its name.
	 * 
	 * @param race String representation of a race.
	 * @return int value of the race, or -1 if not found.
	 */
	private int getRace(String race) {
		switch (race) {
		case "GCP":
			return 0;
		case "Terraner":
			return 1;
		case "Vasudaner":
			return 2;
		case "Shivaner":
			return 3;
		case "Uralte":
			return 4;
		case "Nomads":
			return 5;
		case "NTF":
			return 6;
		case "HoL":
			return 7;
		case "GTU":
			return 8;
		case "Piraten":
			return 9;
		default:
			return -1;
		}
	}
		
	/**
	 * Get the ShipTypeFlag by either its name or description.
	 * 
	 * @param name name or description of the flag.
	 * @return matching flag or null.
	 */
	private ShipTypeFlag getShipFlag(String name) {
		for (ShipTypeFlag flag : ShipTypeFlag.values()) {
			if (flag.getLabel().equals(name) || flag.getDescription().equals(name)) {
				return flag;
			}
		}
		return null;
	}

	/**
	 * Get the weapon Flags by its description.
	 * 
	 * @param desc description of the flag.
	 * @return Flags matching the description, or null.
	 */
	private Flags getWeaponFlag(String desc) {
		switch (desc) {
		case "Beim Angriff zerstört":
			return Flags.DESTROY_AFTER;
		case "Große Reichweite":
			return Flags.LONG_RANGE;
		case "Sehr große Reichweite":
			return Flags.VERY_LONG_RANGE;
		default:
			return null;
		}
	}

	/**
	 * Get internal module string by its name.
	 * Uses the normalized description as id.
	 * 
	 * @param desc description of the module slot
	 * @return id string of the slot.
	 */
	private String getModuleId(String desc) {
		return normalizeString(desc);
	}

	/**
	 * Get internal weapon string by its name.
	 * Uses the normalized name as id.
	 * 
	 * @param name name of the weapon.
	 * @return id string of the weapon.
	 */
	private String getWeaponId(String name) {
		return normalizeString(name);
	}

	private ShipType lastShipType; 
	public ShipType getShipType(){
		return this.lastShipType;
	}
	
	private ShipBaubar lastShipBaubar;
	public ShipBaubar getShipBaubar(){
		return lastShipBaubar;
	}

	@Override
	public void readPage(BufferedReader page, int shipId) throws IOException {
		ShipType shipType = new ShipTypeExt(shipId);
		ShipBaubar shipBaubar = new ShipBaubarExt(shipType);

		String weaponId = "";
		StringBuilder shipFlags = new StringBuilder();
		String section = "";
		String line;
		while ((line = page.readLine()) != null) {
			line = line.trim();
			// TODO: missing: showorderable, showbuildable, Flagschiff
			if ("".equals(shipType.getNickname())
					&& line.startsWith("<span style=\"color:#FFFFFF;font-weight:bold;\">")) {
				shipType.setNickname(subString(line, ">", "</span><br>"));
			} else if(line.equals("<span class=\"verysmallfont\" style=\"color:red;font-style:italic;font-weight:normal\">unsichtbar</span>")) {
				shipType.setHide(true);
			} else if (ShipClasses.UNBEKANNT == shipType.getShipClass()
					&& line.startsWith("<span class=\"verysmallfont\" style=\"font-style:italic\">")) {
				shipType.setShipClass(getShipClass(subString(line, ">", "</span><br>")));
			} else if ("".equals(shipType.getPicture()) && line.startsWith("<img src=\"")
					&& line.endsWith(" alt=\"\">")) {
				// TODO: is it always _files ?
				shipType.setPicture(PICTURE_PATH + subString(line, "_files/", "\" alt=\""));
			} else if (line.equals("<h3>Reaktorwerte</h3>")) {
				section = "reaktor";
			} else if (section.equals("reaktor") && line.endsWith("</td><td class=\"noBorderX\">Uran</td></tr>")) {
				shipType.setRu(toInt(subString(line, "<tr><td class=\"noBorderX\">", "</td><td class=\"noBorderX\"><img")));
			} else if (section.equals("reaktor") && line.endsWith("</td><td class=\"noBorderX\">Deuterium</td></tr>")) {
				shipType.setRd(toInt(subString(line, "<tr><td class=\"noBorderX\">", "</td><td class=\"noBorderX\"><img")));
			} else if (section.equals("reaktor")
					&& line.endsWith("</td><td class=\"noBorderX\">Antimaterie</td></tr>")) {
				shipType.setRa(toInt(subString(line, "<tr><td class=\"noBorderX\">", "</td><td class=\"noBorderX\"><img")));
			} else if (section.equals("reaktor") && line.endsWith("</td><td class=\"noBorderX\">maximal</td></tr>")) {
				shipType.setRm(toInt(subString(line, "<tr><td class=\"noBorderX\">", "</td><td class=\"noBorderX\"><img")));
				section = "";
			} else if (line.equals("<h3>Vorrausetzungen</h3>")) {
				section = "vorraus";
			} else if (section.equals("vorraus") && (
					line.startsWith("<a class=\"ok\" href=\"https://ds2.drifting-souls.net/ds?module=forschinfo&amp;action=default&amp;res=")
					|| line.startsWith("<a class=\"error\" href=\"https://ds2.drifting-souls.net/ds?module=forschinfo&amp;action=default&amp;res="))) {
				Forschung res = getForschung(toInt(subString(line,
						"href=\"https://ds2.drifting-souls.net/ds?module=forschinfo&amp;action=default&amp;res=",
						"\">")));
				res.setName(subString(line, "\">", "</a><br>"));
				if (shipBaubar.getRes(1) == null) {
					shipBaubar.setRes1(res);
				} else if (shipBaubar.getRes(2) == null) {
					shipBaubar.setRes2(res);
				} else if (shipBaubar.getRes(3) == null) {
					shipBaubar.setRes3(res);
				} else {
					unknownLine(line);
				}
			} else if (section.equals("vorraus") && line.startsWith("Rasse: ")) {
				shipBaubar.setRace(getRace(subString(line, "Rasse: ", "<br>")));
				section = "";
			} else if (line.equals("<h3>Produktionskosten</h3>")) {
				section = "bau";
			} else if (section.equals("bau") && line.startsWith("<td class=\"noBorderX\" align=\"left\"><img src=")) {
				Cargo costs = shipBaubar.getCosts();
				if (costs == null) {
					costs = new Cargo();
				}
				String sub = subString(line, "<a class=\"tooltip forschinfo\"", null);
				int count = toInt(subString(sub, "\">", "<span class="));
				costs.addResource(ItemID.fromString(subString(sub, "ds-item-id=\"", "\"><img src=")), count);
				shipBaubar.setCosts(costs);
			} else if (section.equals("bau") && line.startsWith("<tr><td class=\"noBorderX\">Energie</td>")) {
				shipBaubar.setEKosten(toInt(subString(line, "alt=\"\">", "</td></tr>")));
			} else if (section.equals("bau") && line.startsWith("<tr><td class=\"noBorderX\">Besatzung</td>")) {
				shipBaubar.setCrew(toInt(subString(line, "alt=\"\">", "</td></tr>")));
			} else if (section.equals("bau") && line.startsWith("<tr><td class=\"noBorderX\">Dauer</td>")) {
				shipBaubar.setDauer(toInt(subString(line, "alt=\"\">", "</td></tr>")));
			} else if (section.equals("bau") && line.startsWith("<tr><td class=\"noBorderX\">Werftslots</td>")) {
				shipBaubar.setWerftSlots(toInt(subString(line, "alt=\"\">", "</td></tr>")));
				section = "";
			} else if (line.equals("<h3>Bewaffnung</h3>")) {
				section = "waffen";
			} else if (section.equals("waffen") && line.length() > 1 && !line.startsWith("<")) {
				weaponId = getWeaponId(line);
			} else if (section.equals("waffen") && line.startsWith("<span style=")) {
				// TODO: save all known weapons somewhere
				Weapon weapon = new Weapon(weaponId); 
				String sub = subString(line, "'\">", null);
				if (sub.startsWith("AP-Kosten:")) {
					weapon.setApCost(toInt(subString(sub, "AP-Kosten:", "<br>")));
					sub = subString(sub, "<br>", null);
				}
				if (sub.startsWith("Energie-Kosten:")) {
					weapon.setECost(toInt(subString(sub, "Energie-Kosten:", "<br>")));
					sub = subString(sub, "<br>", null);
				}
				if (sub.startsWith("Schüsse:")) {
					weapon.setSingleShots(toInt(subString(sub, "Schüsse:", "<br>")));
					sub = subString(sub, "<br>", null);
				}
				if (sub.startsWith("Max. Überhitzung:")) {
					Map<String, Integer> heat = shipType.getMaxHeat();
					heat.put(weapon.getId(), toInt(subString(sub, "Max. Überhitzung:", "<br>")));
					shipType.setMaxHeat(heat);
					sub = subString(sub, "<br>", null);
				}
				if (sub.startsWith("Schaden (H/S/Sub):")) {
					if (sub.startsWith("Schaden (H/S/Sub): Munition")) {
						Set<String> muni = weapon.getMunitionstypen();
						muni.add("TODO:");
						weapon.setMunitionstypen(muni);
					} else {
						sub = subString(sub, "Schaden (H/S/Sub):", null);
						weapon.setBaseDamage(toInt(subString(sub, null, "/")));
						sub = subString(sub, "/", null);
						weapon.setShieldDamage(toInt(subString(sub, null, "/")));
						weapon.setSubDamage(toInt(subString(sub, "/", "<br>")));
					}
					sub = subString(sub, "<br>", null);
				}
				if (sub.startsWith("Trefferws (C/J/Torp):")) {
					if (sub.startsWith("Trefferws (C/J/Torp): Munition")) {
						Set<String> muni = weapon.getMunitionstypen();
						muni.add("TODO:");
						weapon.setMunitionstypen(muni);
					} else {
						sub = subString(sub, "Trefferws (C/J/Torp):", null);
						weapon.setDefTrefferWS(toInt(subString(sub, null, "/")));
						sub = subString(sub, "/", null);
						weapon.setDefSmallTrefferWS(toInt(subString(sub, null, "/")));
						weapon.setTorpTrefferWS(Double.parseDouble(subString(sub, "/", "<br>")));
					}
					sub = subString(sub, "<br>", null);
				}

				while (!sub.equals("</span>")) {
					Set<Flags> flags = weapon.getFlags();
					flags.add(getWeaponFlag(subString(sub, null, "<br>")));
					weapon.setFlags(flags);
					sub = subString(sub, "<br>", null);
				}
			} else if (section.equals("waffen") && line.startsWith("<td class=\"noBorderX\">")) {
				Map<String, Integer> weapons = shipType.getWeapons();
				weapons.put(weaponId, toInt(subString(line, "<td class=\"noBorderX\">", "</td>")));
				shipType.setWeapons(weapons);
			} else if (section.equals("waffen") && (line.equals("</tbody></table>") || line.equals("<tbody><tr><td class=\"noBorderX\">-keine-</td></tr>"))) {
				section = "";
			} else if (line.equals("<h3>Antrieb</h3>")) {
				section = "antrieb";
			} else if (section.equals("antrieb") && line.contains("Flugkosten<br>")) {
				shipType.setCost(toInt(subString(line, null, " Flugkosten<br>")));
				shipType.setHeat(toInt(subString(line, "<br>", " Überhitzung<br><br>")));
			} else if (section.equals("antrieb") && line.startsWith("Größe/Beweglichkeit:")) {
				shipType.setSize(toInt(subString(line, "Größe/Beweglichkeit:", "<br>")));
			} else if (section.equals("antrieb") && line.startsWith("Sensorreichweite:")) {
				shipType.setSensorRange(toInt(subString(line, "Sensorreichweite:", "<br><br>")) - 1);
			} else if (line.equals("<h3>Ausstattung</h3>")) {
				section = "stat";
			} else if (section.equalsIgnoreCase("stat") && line.contains("Energiespeicher<br>")) {
				shipType.setEps(toInt(subString(line, null, "Energiespeicher<br>")));
				shipType.setCargo(toInt(subString(line, "Energiespeicher<br>", "Cargo<br>")));
				shipType.setCrew(toInt(subString(line, "Cargo<br>", "Kabinen<br>")));
				section = "";
			} else if (line.endsWith("Jägerdocks<br>")) {
				shipType.setJDocks(toInt(subString(line, null, "Jägerdocks<br>")));
			} else if(line.startsWith("Externe Dockinganlage (Kapazität:")){
				shipType.setADocks(toInt(subString(line, "Externe Dockinganlage (Kapazität:", ")<br>")));
			} else if (line.equals("<h1>Modulsteckplätze</h1>")) {
				section = "module";
			} else if (section.equals("module") && line.contains("<br>")) {
				String sub = line;
				int counter = 1;
				StringBuilder modules = new StringBuilder();
				while (sub.contains("<br>")) {
					modules.append(counter).append(":");
					modules.append(getModuleId(subString(sub, null, "<br>"))).append(";");
					sub = subString(sub, "<br>", null);
					counter++;
				}
				shipType.setModules(modules.substring(0, modules.length() - 1));
				section = "";
			} else if(line.endsWith("Werftslots<br>")) {
				shipType.setWerft(toInt(subString(line, null, "Werftslots<br>")));
			} else if (line.startsWith("<br>Hüllenstärke: ")) {
				shipType.setHull(toInt(subString(line, "Hüllenstärke: ", "<br>")));
			} else if (line.startsWith("Ablative Panzerung: ")) {
				shipType.setAblativeArmor(toInt(subString(line, "Ablative Panzerung: ", "<br>")));
			} else if (line.startsWith("Panzerung:")) {
				shipType.setPanzerung(toInt(subString(line, "Panzerung:", "<br>")));
			} else if(line.startsWith("Schildstärke:")){
				shipType.setShields(toInt(subString(line, "Schildstärke:", "<br>")));
			} else if (line.startsWith("Platz für Einheiten:")) {
				shipType.setUnitSpace(toInt(subString(line, "Platz für Einheiten:", "<br>")));
			} else if (line.startsWith("Maximale Einheitengröße:")) {
				shipType.setMaxUnitSize(toInt(subString(line, "Maximale Einheitengröße:", "<br>")));
			} else if (line.startsWith("Nahrungsspeicher:")) {
				shipType.setNahrungCargo(toInt(subString(line, "Nahrungsspeicher:", "<br>")));
			} else if(line.startsWith("Tanker: <img")) {
				shipType.setDeutFactor(toInt(subString(line, "Deuterium\">", " für <img")));
			} else if(line.startsWith("Produziert: <img") && line.contains("alt=\"Nahrung\">")){
				shipType.setHydro(toInt(subString(line, "alt=\"Nahrung\">", "<br>")));
			} else if(line.startsWith("Torpedoabwehr:")){
				shipType.setTorpedoDef(toInt(subString(line, "Torpedoabwehr:", "%<br>")));
			} else if (line.startsWith("Betriebskosten: ")) {
				shipType.setReCost(toInt(subString(line, "Betriebskosten: ", "RE")));
			} else if (getShipFlag(line) != null) {
				section = "flag";
				shipFlags.append(getShipFlag(line).getFlag()).append(" ");
			} else if(section.equals("flag") && !line.equals("</span>")){
				// continue
				// TODO: reliable to ignore !only! flag description?
			} else if(section.equals("flag") && line.equals("</span>")){
				section = "";
			} else if (line.equals("<h3>Beschreibung</h3>")) {
				section = "besch";
			} else if (section.equals("besch") && line.equals("</td>")) {
				section = "";
			} else if (section.equals("besch")) {
				shipType.setDescrip(shipType.getDescrip() + line + " ");
			} else {
				unknownLine(line);
			}
		}
		if(shipFlags.length() > 1){
			shipType.setFlags(shipFlags.substring(0, shipFlags.length() - 1));
		}
		shipType.setVersorger(shipType.hasFlag(ShipTypeFlag.VERSORGER));
		shipType.setDescrip(shipType.getDescrip().trim());
		shipType.setDescrip(convertHtmlToBBCode(shipType.getDescrip()));

		guessValues(shipType);
		
		this.lastShipType = shipType;
		this.lastShipBaubar = shipBaubar;
	}	

	/**
	 * Guess values that aren't part of the SchiffInfo page.
	 * 
	 * @param shipType shipType object that will be modified.
	 * @return the same reference of the modified shipType.
	 */
	private ShipType guessValues(ShipType shipType) {
		// not used/implemented
		shipType.setBounty(BigInteger.ZERO);
		// not used/implemented
		shipType.setChance4Loot(0);
		// guessed from git samples
		shipType.setGroupwrap(shipType.getShipClass() == ShipClasses.GESCHUETZ ? 5 : 10);
		// guessed from git samples
		shipType.setLostInEmpChance(shipType.getShipClass() == ShipClasses.AWACS ? 0.5 : 0.75);
		// totally wrong. Need to test every single ship in live ds
		shipType.setMinCrew((int) (shipType.getCrew() * 0.6));
		// guess thats how its used
		shipType.setSrs(shipType.getSensorRange() > 0);
		// TODO: Einwegwerft?
		return shipType;
	}

	/**
	 * Process unknown lines. Ignore expected unused lines, and output
	 * unexpected lines.
	 * 
	 * @param line line to check.
	 */
	private void unknownLine(String line) {
		if (line == null || line.isEmpty() 
				|| line.endsWith("<head>")
				|| line.startsWith("<title>")
				|| line.startsWith("<meta")
				|| line.startsWith("<link rel=\"stylesheet\"")
				|| line.startsWith("<!")
				|| line.endsWith(" -->")
				|| line.startsWith("<style type")
				|| line.equals("<body>")
				|| line.startsWith("<input name=")
				|| line.startsWith("<table")
				|| line.startsWith("<div ")
				|| line.equals("<tbody><tr>")
				|| (line.startsWith("<td ") && line.length() < 50)
				|| line.equals("<br>")
				|| line.equals("<tr>")
				|| line.startsWith("<hr ")
				|| line.startsWith("</")
				|| line.equals("<a class=\"tooltip forschinfo\" href=\"#\">")
				|| line.equals("<a href=\"#\" class=\"forschinfo tooltip\">")
				|| line.equals("<span class=\"ttcontent\">")
				|| line.endsWith("Modulsteckplätze")
				|| line.contains("info.gif")
				|| line.startsWith("<td class=\"noBorderX\"><a class=\"tooltip forschinfo\"")
//				|| getShipFlag(line) != null // TODO: line breaks
				|| line.contains("parent.")) {
			return;
		} else {
			System.out.println("Unknown line: " + line);
		}
	}
	
	/**
	 * A toString method for ShipType to display all its attributes. 
	 * 
	 * @param ship the ShipType to print.
	 * @return String representation of ship.
	 */
	@Deprecated
	public String toString(ShipType ship) {
		StringBuilder builder = new StringBuilder();
		builder.append("Id: ").append(ship.getId()).append(" | ");
		builder.append("ADocks: ").append(ship.getADocks()).append(" | ");
		builder.append("Ablative Panzerung: ").append(ship.getAblativeArmor()).append(" | ");
		builder.append("Bounty: ").append(ship.getBounty()).append(" | ");
		builder.append("Cargo: ").append(ship.getCargo()).append(" | ");
		builder.append("Loot: ").append(ship.getChance4Loot()).append(" | ");
		builder.append("Flugkosten: ").append(ship.getCost()).append(" | ");
		builder.append("Crew: ").append(ship.getCrew()).append(" | ");
		builder.append("Beschreibung: ").append(ship.getDescrip()).append(" | ");
		builder.append("Deut sammeln: ").append(ship.getDeutFactor()).append(" | ");
		builder.append("Energieprod: ").append(ship.getEps()).append(" | ");
		builder.append("Flags: ").append(ship.getFlags()).append(" | ");
		builder.append("Gruppieren: ").append(ship.getGroupwrap()).append(" | ");
		builder.append("Überhitzung: ").append(ship.getHeat()).append(" | ");
		builder.append("Versteckt: ").append(ship.isHide()).append(" | ");
		builder.append("Hülle: ").append(ship.getHull()).append(" | ");
		builder.append("Nahrungsprod: ").append(ship.getHydro()).append(" | ");
		builder.append("Jägerdocks: ").append(ship.getJDocks()).append(" | ");
		builder.append("EMP Lost: ").append(ship.getLostInEmpChance()).append(" | ");
		builder.append("Waffen Hitze: ").append(ship.getMaxHeat()).append(" | ");
		builder.append("Einheitengröße: ").append(ship.getMaxUnitSize()).append(" | ");
		builder.append("Min Crew: ").append(ship.getMinCrew()).append(" | ");
		builder.append("Module: ").append(ship.getModules()).append(" | ");
		builder.append("Nahrungsspeicher: ").append(ship.getNahrungCargo()).append(" | ");
		builder.append("Name: ").append(ship.getNickname()).append(" | ");
		builder.append("Panzerung: ").append(ship.getPanzerung()).append(" | ");
		builder.append("Bild: ").append(ship.getPicture()).append(" | ");
		builder.append("Reaktor AM: ").append(ship.getRa()).append(" | ");
		builder.append("Reaktor Deut: ").append(ship.getRd()).append(" | ");
		builder.append("Unterhalt: ").append(ship.getReCost()).append(" | ");
		builder.append("Reaktor Max: ").append(ship.getRm()).append(" | ");
		builder.append("Reaktor Uran: ").append(ship.getRu()).append(" | ");
		builder.append("Sensorreichweite: ").append(ship.getSensorRange()).append(" | ");
		builder.append("Schilde: ").append(ship.getShields()).append(" | ");
		builder.append("ShipClass: ").append(ship.getShipClass()).append(" | ");
		builder.append("Größe: ").append(ship.getSize()).append(" | ");
		builder.append("SRS Scanner: ").append(ship.hasSrs()).append(" | ");
		builder.append("TorpedoDef: ").append(ship.getTorpedoDef()).append(" | ");
		builder.append("Einheitenladeraum: ").append(ship.getUnitSpace()).append(" | ");
		builder.append("Version: ").append(ship.getVersion()).append(" | ");
		builder.append("Versorger: ").append(ship.isVersorger()).append(" | ");
		builder.append("Waffen: ").append(ship.getWeapons()).append(" | ");
		builder.append("Werftslots: ").append(ship.getWerft()).append(" | ");
		builder.append("Einwegwerft: ").append(ship.getOneWayWerft());

		return builder.toString();
	}
	
	/**
	 * A toString method for ShipBaubar to display all its attributes.
	 * 
	 * @param ship the ShipBaubar to print.
	 * @return String representation of ship.
	 */
	@Deprecated
	public String toString(ShipBaubar ship) {
		StringBuilder builder = new StringBuilder();
		builder.append("Id: ").append(ship.getId()).append(" | ");
		builder.append("Baukosten: ").append(ship.getCosts()).append(" | ");
		builder.append("Crew: ").append(ship.getCrew()).append(" | ");
		builder.append("Baudauer: ").append(ship.getDauer()).append(" | ");
		builder.append("Energiekosten: ").append(ship.getEKosten()).append(" | ");
		builder.append("Flagschiff: ").append(ship.isFlagschiff()).append(" | ");
		builder.append("Rasse: ").append(ship.getRace()).append(" | ");
		builder.append("Werftslots: ").append(ship.getWerftSlots()).append(" | ");
		builder.append("Forschungen: ").append(ship.getBenoetigteForschungen()).append(" | ");
		builder.append("ShipType: ").append(ship.getType().getId());

		return builder.toString();
	}
}