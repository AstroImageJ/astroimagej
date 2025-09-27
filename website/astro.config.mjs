import { defineConfig } from 'astro/config';
import starlight from '@astrojs/starlight';

export default defineConfig({
    site: 'https://astroimagej.com',
    integrations: [
        starlight({
            title: 'AstroImageJ Docs',
            social: [
                { icon: 'github', label: 'GitHub', href: 'https://github.com/AstroImageJ/astroimagej' },
                { icon: 'comment', label: 'Github Discussions', href: 'https://github.com/AstroImageJ/astroimagej/discussions' },
                { icon: 'comment-alt', label: 'Nabble Forum', href: 'http://astroimagej.170.s1.nabble.com/' },
            ],
            editLink: {
                baseUrl: 'https://github.com/AstroImageJ/astroimagej/edit/master/website/',
            },
            components: {
                SocialIcons: './src/components/StarlightSocialIcons.astro',
            },
        }),
    ],
});