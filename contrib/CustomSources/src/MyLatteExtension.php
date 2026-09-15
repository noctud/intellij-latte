<?php declare(strict_types=1);

namespace IdeaLatteSupport;

class MyLatteExtension extends \Latte\Extension
{

	/** @inheritdoc */
	public function getTags(): array
	{
		return [
			'normal' => function () {},
			'n:attrOnly' => function () {},
			'pair' => function (): \Generator {},
		];
	}

	/** @inheritdoc */
	public function getFilters(): array
	{
		return [
			'myFilter' => function ($param) {},
		];
	}

	/** @inheritdoc */
	public function getFunctions(): array
	{
		return [
			'myFunction' => function ($param): bool {},
		];
	}

}
