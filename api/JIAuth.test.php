<?php

require 'JIAuth.class.php';

$clientId = "first_app";
$clientSecret = "first_app_secret";

$auth = new JIAuth("http://localhost:9000", $clientId, $clientSecret);

echo "\n-- All should fail\n";

print_r ($auth->get_app_ticket("ticket"));
echo ($auth->add_user_to_group("group", 4)) . "\n";
echo ($auth->remove_user_from_group("group", 4))  . "\n";
print_r ($auth->login("wtf"));

echo "\n-- Should suceed\n";

$token = $auth->login($clientId);

print_r($token);
print_r($auth->get_app_ticket($token[1]));

echo "\n-- Should suceed\n";

echo var_dump($auth->add_user_to_group("group", 6))  . "\n";
echo var_dump($auth->remove_user_from_group("group", 6))  . "\n";

echo "\n-- Should fail\n";

echo var_dump($auth->add_user_to_group("other_group", 6))  . "\n";
echo var_dump($auth->remove_user_from_group("other_group", 6));
