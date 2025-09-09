import { defineConfig } from 'astro/config';
import starlight from '@astrojs/starlight';

export default defineConfig({
    site: 'https://astroimagej.github.io',
    base: '/astroimagej',
    integrations: [
        starlight({
            title: 'AstroImageJ Docs',
        }),
    ],
});