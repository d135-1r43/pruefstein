import adapter from '@sveltejs/adapter-static';

/** @type {import('@sveltejs/kit').Config} */
const config = {
	kit: {
		// Fully static output: `npm run build` writes plain HTML/CSS/JS to
		// build/, which is what pruefstein.com serves. No Node runtime needed.
		adapter: adapter({
			pages: 'build',
			assets: 'build',
			fallback: '404.html',
			precompress: false,
			strict: true
		}),
		// GitHub Pages serves this from /<repo>/, so the whole site has to know
		// it lives under a prefix. Empty locally and on a future apex domain.
		paths: {
			base: process.env.BASE_PATH ?? ''
		},
		prerender: {
			handleHttpError: 'fail'
		}
	}
};

export default config;
