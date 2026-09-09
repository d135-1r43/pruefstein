<script>
	/**
	 * The Prüfstein mark: a touchstone, drawn as a faceted stone.
	 *
	 * Geometry is `static/logo.svg`, unaltered. Two things differ from that
	 * file, both so one mark can sit anywhere on the page:
	 *
	 *  - the white background square is dropped, so the mark works on the paper,
	 *    on the accent band and on the black footer;
	 *  - the fill is `currentColor`, so a caller inverts it by setting `color`.
	 *
	 * The viewBox is tightened from the source file's `0 0 1254 1254` to the
	 * artwork's measured bounds plus a hair of bleed, so the stone fills the box
	 * it is given instead of floating in the file's padding. The artwork is a
	 * little wider than tall, so the height is derived rather than assumed.
	 */
	let { size = 38, title = '' } = $props();

	const RATIO = 997 / 1075;
</script>

<svg
	width={size}
	height={Math.round(size * RATIO)}
	viewBox="90 124 1075 997"
	fill="currentColor"
	role={title ? 'img' : 'presentation'}
	aria-hidden={title ? undefined : 'true'}
	aria-label={title || undefined}
	xmlns="http://www.w3.org/2000/svg"
>
	{#if title}<title>{title}</title>{/if}
	<path
		fill-rule="evenodd"
		d="M657 132 Q668 126 678 134 L912 339 Q919 345 922 354 L1032 624 1051 683 1078 718 1156 863 Q1161 872 1155 882 L1066 1037 Q1062 1044 1054 1048 L890 1115 365 1115 183 1019 Q177 1016 174 1007 L97 826 Q94 820 96 812 L124 661 Q125 654 131 649 L243 553 251 453 Q251 445 258 440 L354 369 454 226 Q458 221 464 218Z
		   M645 194 494 259 433 348 507 374 658 302Z
		   M702 220 717 332 806 565 987 666 877 378Z
		   M394 403 304 468 298 553 401 621 461 428Z
		   M520 426 444 671 484 895 692 765 749 586 672 357Z
		   M272 599 176 675 151 805 223 978 335 1039 434 921 390 679Z
		   M797 622 750 774 896 948 1089 861 1037 759Z
		   M472 960 387 1064 822 1064 844 972 717 816Z
		   M899 1000 883 1063 1028 1009 1076 921Z"
	/>
</svg>
