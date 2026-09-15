<?php declare(strict_types=1);

namespace IdeaLatteSupport;

final class Generator
{

	public static function generate(\Latte\Extension ...$extensions): string
	{
		$filters = [];
		$functions = [];
		$tags = [];

		foreach ($extensions as $extension) {
			$filters += $extension->getFilters();
			$functions += $extension->getFunctions();
			$tags += $extension->getTags();
		}

		$params = [
			'filters' => self::getCallables($filters),
			'functions' => self::getCallables($functions),
			'macros' => [],
		];

		foreach ($tags as $name => $tag) {
			$name = preg_replace('~^n:~', '', $name, count: $count) ?? $name;
			$pair = null;
			if (is_callable($tag)) {
				$r = new \ReflectionFunction(\Closure::fromCallable($tag));
				$pair = (string) $r->getReturnType() === 'Generator' ? 'PAIR' : null;
			}
			$params['macros'][$name] = $count === 1 ? 'ATTR_ONLY' : $pair;
		}

		$latte = new \Latte\Engine();
		return $latte->renderToString(__DIR__ . '/idea.latte', $params);
	}

	/**
	 * @param array<callable(): mixed> $callables
	 * @return array<array{returns: string, params: string}>
	 */
	private static function getCallables(array $callables): array
	{
		$return = [];

		foreach ($callables as $name => $callable) {
			$r = new \ReflectionFunction(\Closure::fromCallable($callable));
			$return[$name] = [
				'returns' => (string) $r->getReturnType(),
				'params' => implode(', ', array_column($r->getParameters(), 'name')),
			];
		}

		return $return;
	}

}
