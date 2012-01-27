package jdos.win.builtin.user32;

public class Hook {
    static public int HOOK_CallHooks(int id, int code, int wparam, int lparam) {
//        struct user_thread_info *thread_info = get_user_thread_info();
//        struct hook_info info;
//        DWORD_PTR ret = 0;
//
//        USER_CheckNotLock();
//
//        if (!HOOK_IsHooked( id ))
//        {
//            TRACE( "skipping hook %s mask %x\n", hook_names[id-WH_MINHOOK], thread_info->active_hooks );
//            return 0;
//        }
//
//        ZeroMemory( &info, sizeof(info) - sizeof(info.module) );
//        info.prev_unicode = unicode;
//        info.id = id;
//
//        SERVER_START_REQ( start_hook_chain )
//        {
//            req->id = info.id;
//            req->event = EVENT_MIN;
//            wine_server_set_reply( req, info.module, sizeof(info.module)-sizeof(WCHAR) );
//            if (!wine_server_call( req ))
//            {
//                info.module[wine_server_reply_size(req) / sizeof(WCHAR)] = 0;
//                info.handle       = wine_server_ptr_handle( reply->handle );
//                info.pid          = reply->pid;
//                info.tid          = reply->tid;
//                info.proc         = wine_server_get_ptr( reply->proc );
//                info.next_unicode = reply->unicode;
//                thread_info->active_hooks = reply->active_hooks;
//            }
//        }
//        SERVER_END_REQ;
//
//        if (!info.tid && !info.proc) return 0;
//        ret = call_hook( &info, code, wparam, lparam );
//
//        SERVER_START_REQ( finish_hook_chain )
//        {
//            req->id = id;
//            wine_server_call( req );
//        }
//        SERVER_END_REQ;
//        return ret;
        return 0;
    }
}
