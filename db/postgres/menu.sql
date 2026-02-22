BEGIN;
insert into public.menu (id, component, disabled, enable_multi_tab, extra, icon, key, label, name, parent_id, path,
                         pinned, redirect, show, show_tab, sorting_order)
values (1, 'dashboard/index', false, false, null, 'icon-[mage--dashboard-chart]', 'dashboard', '仪表板', 'dashboard',
        null, 'dashboard', true, '', true, true, 0),
       (2, 'dataset', false, false, null, 'icon-[ic--outline-dataset]', 'dataset', '数据集', 'dataset', null, 'dataset',
        false, '', true, true, 0),
       (3, 'dataset/type/index', false, false, null, 'icon-[game-icons--barbed-star]', 'type', '属性管理', 'type', 2,
        'type', false, '', true, true, 0),
       (4, 'dataset/type-damage-relation/index', false, false, null, 'icon-[game-icons--beveled-star]',
        'type-damage-relation', '属性克制管理', 'type-damage-relation', 2, 'type-damage-relation', false, '', true,
        true, 0),
       (5, 'dataset/berry-firmness/index', false, false, null, 'icon-[game-icons--diamond-hard]', 'berry-firmness',
        '树果硬度管理', 'berry-firmness', 2, 'berry-firmness', false, '', true, true, 0),
       (6, 'dataset/berry-flavor/index', false, false, null, 'icon-[game-icons--opened-food-can]', 'berry-flavor',
        '树果风味管理', 'berry-flavor', 2, 'berry-flavor', false, '', true, true, 0),
       (7, 'dataset/berry/index', false, false, null, 'icon-[game-icons--elderberry]', 'berry', '树果管理', 'berry', 2,
        'berry', false, '', true, true, 0);
COMMIT;