<?php

// Error definitions

define('E_UNKNOWN_ERROR', 100);
define('E_MISSING_DATA', 101);
define('E_UNKNOWN_APP', 102);
define('E_INVALID_APP_SECRET', 103);
define('E_INVALID_CAPTCHA', 104);

define('E_INVALID_TICKET', 201);
define('E_GROUP_NOT_FOUND', 201);
define('E_USER_NOT_FOUND', 202);
define('E_MISSING_PERMISSION', 203);

// Ticket types definitions

define('T_LOGIN', 'T_LOGIN');
define('T_REGISTER', 'T_REGISTER');
define('T_DOUBLE_REGISTER', 'T_DOUBLE_REGISTER');
define('T_EMAIL_CONFIRM', 'T_EMAIL_CONFIRM');
define('T_PASSWORD_RESET', 'T_PASSWORD_RESET');
define('T_EXPLICIT_GRANT', 'T_EXPLICIT_GRANT');
define('T_APP', 'T_APP');

define('VALID_LOGINS', array(T_LOGIN, T_EMAIL_CONFIRM, T_PASSWORD_RESET, T_EXPLICIT_GRANT, T_APP));


class JIAuth {
	private $appBaseUrl;
	private $appClientId;
	private $appClientSecret;

	public function __construct($appBaseUrl, $appClientId, $appClientSecret) {
		$this->appBaseUrl = $appBaseUrl;
		$this->appClientId = $appClientId;
		$this->appClientSecret = $appClientSecret;
	}

	/**
	  * Get an app ticket
	  * @return an array with (userId -> int, userEmail -> string, ticketType -> string, groups -> array,
	  *     user -> array(
	  *        id -> int,
	  *        email -> string,
	  *        details -> array(firstName -> string, lastName -> string, phoneNumber -> nullable string),
	  *        address -> array(address > string, addressComplement -> nullable string, postCode -> string, region -> string, country -> string)
	  *     ))
	  * or with "errorCode"
	  */
	public function get_app_ticket($ticket) {
		return $this->request("api/ticket/" . $ticket, "GET");
	}

	/**
    	  * Get an user profile
    	  * @return an array (
    	  *        id -> int,
    	  *        email -> string,
    	  *        details -> array(firstName -> string, lastName -> string, phoneNumber -> nullable string),
    	  *        address -> array(address > string, addressComplement -> nullable string, postCode -> string, region -> string, country -> string)
    	  *     )
    	  * or with "errorCode"
    	  */
	public function get_user_info($userId) {
		return $this->request("api/user/" . $userId, "GET");
	}

	public function add_user_to_group($group, $userId) {
		$req_result = $this->request("api/groups/" . $group . "/members", "POST", array("userId" => $userId + 0));

		if (isset($req_result["errorCode"])) {
			return $req_result["errorCode"];
		}

		return TRUE;
	}

	public function remove_user_from_group($group, $userId) {
		$req_result = $this->request("api/groups/" . $group . "/members/" . $userId, "DELETE");

		if (isset($req_result["errorCode"])) {
			return $req_result["errorCode"];
		}

		return TRUE;
	}

	public function login($otherAppClientId) {
		$req_result = $this->request("api/app_login/" . $otherAppClientId, "GET");

		if (!isset($req_result["ticket"])) {
			return array(FALSE, $req_result["errorCode"]);
		}

		return array(TRUE, $req_result["ticket"]);
	}

	private function request($url, $method, $data = array()) {
		$url = $this->appBaseUrl . "/" . $url;

	    $ch = curl_init($url);

		curl_setopt($ch, CURLOPT_CUSTOMREQUEST, $method);

		$header = array(
			"X-Client-Id: " . $this->appClientId, 
			"X-Client-Secret: " . $this->appClientSecret
		);     
		
		curl_setopt($ch, CURLOPT_RETURNTRANSFER, true);     

		if (sizeof($data) > 0) {
			$data_string = json_encode($data);  
			curl_setopt($ch, CURLOPT_POSTFIELDS, $data_string);
			curl_setopt($ch, CURLOPT_POST, TRUE);

			$header[] = 'Content-Type: application/json';
			$header[] = 'Content-Length: ' . strlen($data_string);
		}

	    curl_setopt($ch, CURLOPT_HTTPHEADER, $header);

	    $result = curl_exec($ch);

	    curl_close($ch);
	    return json_decode($result, TRUE);
	}
}
