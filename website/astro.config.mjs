import { defineConfig } from 'astro/config';
import starlight from '@astrojs/starlight';

export default defineConfig({
    site: 'https://astroimagej.com',
    integrations: [
        starlight({
            title: 'AstroImageJ Docs',
            social: [
                { icon: 'seti:csv', label: 'Home', href: 'https://astroimagej.com/' },
                { icon: 'github', label: 'GitHub', href: 'https://github.com/AstroImageJ/astroimagej' },
                { icon: 'comment', label: 'Github Discussions', href: 'https://github.com/AstroImageJ/astroimagej/discussions' },
                { icon: 'comment-alt', label: 'Nabble Forum', href: 'http://astroimagej.170.s1.nabble.com/' },
            ],
        }),
    ],
});