import { defineConfig } from 'astro/config';
import starlight from '@astrojs/starlight';

export default defineConfig({
    site: 'https://astroimagej.com',
    integrations: [
        starlight({
            title: 'AstroImageJ Docs',
        }),
    ],
});