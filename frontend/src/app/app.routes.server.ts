import { RenderMode, ServerRoute } from '@angular/ssr';
import { routesIDs } from './routes-ids';

export const serverRoutes: ServerRoute[] = [

  // ✅ STATIC ROUTE (MANDATORY)
  {
    path: '',
    renderMode: RenderMode.Prerender,
  },

  // ✅ DYNAMIC ROUTE (OPTIONAL PRERENDER)
  {
    path: 'product/:id',
    renderMode: RenderMode.Prerender,
    async getPrerenderParams() {
      return routesIDs.map(id => ({ id }));
    },
  },

  // ✅ WILDCARD MUST BE SSR (NOT PRERENDER)
  {
    path: '**',
    renderMode: RenderMode.Server
  }
];
