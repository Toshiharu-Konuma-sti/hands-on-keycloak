package jp.sios.apisl.handson.keycloak.ressrv.controller;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import java.util.Map;

@RestController
public class ResourceServerRestController {

//	api-svr
//	`-- /apis
//	    `-- /sports
//	        |-- /baseball
//	        `-- /soccer
//	            |-- /managers
//	            `-- /players

	// {{{ public Map<String, String>[] apis()
	@RequestMapping("/apis")
	public Map<String, String>[] apis() {
		Map<String, String> map[] = new Map[1];
		map[0] = Map.of("id", "1", "path", "sports");
		return map;
	}
	// }}}

	// {{{ public Map<String, String>[] apisSports()
	@RequestMapping("/apis/sports")
	public Map<String, String>[] apisSports() {
		Map<String, String> map[] = new Map[2];
		map[0] = Map.of("id", "1", "path", "baseball");
		map[1] = Map.of("id", "2", "path", "soccer");
		return map;
	}
	// }}}

	// {{{ public Map<String, String>[] apisSportsBaseball()
	@RequestMapping("/apis/sports/baseball")
	public Map<String, String>[] apisSportsBaseball() {
		Map<String, String> map[] = new Map[2];
		map[0] = Map.of("id", "1", "path", "apis/sports/baseball/managers");
		map[1] = Map.of("id", "2", "path", "apis/sports/baseball/players");
		return map;
	}
	// }}}

	// {{{ public Map<String, String>[] apisSportsBaseballManagers()
	@RequestMapping("/apis/sports/baseball/managers")
	public Map<String, String>[] apisSportsBaseballManagers() {
		Map<String, String> map[] = new Map[7];
		map[0] = Map.of("id", "1", "name", "Sadaharu Oh");
		map[1] = Map.of("id", "2", "name", "Senichi Hoshino");
		map[2] = Map.of("id", "3", "name", "Tatsunori Hara");
		map[3] = Map.of("id", "4", "name", "Koji Yamamoto");
		map[4] = Map.of("id", "5", "name", "Kokubo Hiroki");
		map[5] = Map.of("id", "6", "name", "Atsunori Inaba");
		map[6] = Map.of("id", "7", "name", "Hideki Kuriyama");
		return map;
	}
	// }}}

	// {{{ public Map<String, String>[] apisSportsBaseballPlayers()
	@RequestMapping("/apis/sports/baseball/players")
	public Map<String, String>[] apisSportsBaseballPlayers() {
		Map<String, String> map[] = new Map[9];
		map[0] = Map.of("id", "1", "name", "Lars Nootbaar");
		map[1] = Map.of("id", "2", "name", "Kensuke Kondo");
		map[2] = Map.of("id", "3", "name", "Shohei Ohtani");
		map[3] = Map.of("id", "4", "name", "Munetaka Murakami");
		map[4] = Map.of("id", "5", "name", "Masataka Yoshida");
		map[5] = Map.of("id", "6", "name", "Kazuma Okamoto");
		map[6] = Map.of("id", "7", "name", "Shugo Maki");
		map[7] = Map.of("id", "8", "name", "Sosuke Genda");
		map[8] = Map.of("id", "9", "name", "Yu Darvish");
		return map;
	}
	// }}}

	// {{{ public Map<String, String>[] apisSportsSoccer()
	@RequestMapping("/apis/sports/soccer")
	public Map<String, String>[] apisSportsSoccer() {
		Map<String, String> map[] = new Map[2];
		map[0] = Map.of("id", "1", "path", "apis/sports/soccer/managers");
		map[1] = Map.of("id", "2", "path", "apis/sports/soccer/players");
		return map;
	}
	// }}}

	// {{{ public Map<String, String>[] apisSportsSoccerManagers()
	@RequestMapping("/apis/sports/soccer/managers")
	public Map<String, String>[] apisSportsSoccerManagers() {
		Map<String, String> map[] = new Map[7];
		map[0] = Map.of("id", "1", "name", "Takeshi Okada");
		map[1] = Map.of("id", "2", "name", "Philippe Troussier");
		map[2] = Map.of("id", "3", "name", "Zico");
		map[3] = Map.of("id", "4", "name", "Takeshi Okada");
		map[4] = Map.of("id", "5", "name", "Alberto Zaccheroni");
		map[5] = Map.of("id", "6", "name", "Akira Nishino");
		map[6] = Map.of("id", "7", "name", "Hajime Moriyasu");
		return map;
	}
	// }}}

	// {{{ public Map<String, String>[] apisSportsSoccerPlayers()
	@RequestMapping("/apis/sports/soccer/players")
	public Map<String, String>[] apisSportsSoccerPlayers() {
		Map<String, String> map[] = new Map[11];
		map[0] = Map.of("id", "1", "name", "Shuichi gonda");
		map[1] = Map.of("id", "2", "name", "Shogo Taniguchi");
		map[2] = Map.of("id", "3", "name", "Koh Itakur");
		map[3] = Map.of("id", "4", "name", "Yuto Nagatomo");
		map[4] = Map.of("id", "5", "name", "Maya Yoshida");
		map[5] = Map.of("id", "6", "name", "Takefusa Kubo");
		map[6] = Map.of("id", "7", "name", "Hidemasa Morita");
		map[7] = Map.of("id", "8", "name", "Junya Ito");
		map[8] = Map.of("id", "9", "name", "Daichi Kamada");
		map[9] = Map.of("id", "10", "name", "Ao Tanaka");
		map[10] = Map.of("id", "11", "name", "Daizen Maeda");
		return map;
	}
	// }}}


	@RequestMapping("/happy")
	public String happy() {
		return "Hello at happy!";
	}

	@RequestMapping("/test1")
	public String test1() {
		return "Hello at test1!";
	}

	@RequestMapping("/test2")
	public String test2() {
		return "Hello at test2!";
	}

	@RequestMapping("/test3")
	public String test3() {
		return "Hello at test3!";
	}

	@RequestMapping("/test4")
	public String test4() {
		return "Hello at test4!";
	}

	@RequestMapping("/test5")
	public String test5() {
		return "Hello at test5!";
	}

	@RequestMapping("/test6")
	public String test6() {
		return "Hello at test6!";
	}

	@RequestMapping("/test7")
	public String test7() {
		return "Hello at test7!";
	}

}
