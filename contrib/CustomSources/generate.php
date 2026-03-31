#!/usr/bin/env php
<?php declare(strict_types=1);

namespace IdeaLatteSupport;

require_once __DIR__ . '/vendor/autoload.php';

$extensions = [new MyLatteExtension()];

file_put_contents('latte.xml', Generator::generate(...$extensions));
echo "latte.xml saved.\n";
