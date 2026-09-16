package dev.silentsean.mod.devsession.common.auth;

import dev.silentsean.mod.devsession.common.DevSession;
import dev.silentsean.mod.devsession.common.auth.microsoft.MicrosoftAuthProvider;

public interface IAuthProviderFactory {

    IAuthProviderFactory MICROSOFT = MicrosoftAuthProvider::new;

    IAuthProvider create(DevSession DevSession);

}
