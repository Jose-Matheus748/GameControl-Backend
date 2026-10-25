package com.gamecontrol.enums;

/**
 * Resultado de "seguir" um usuário.
 * <ul>
 *   <li>{@link #FOLLOWING}: o usuário passou a seguir (perfil público, ou já seguia).</li>
 *   <li>{@link #REQUESTED}: o perfil é privado e ficou um pedido pendente de aprovação.</li>
 * </ul>
 */
public enum FollowResult {
    FOLLOWING,
    REQUESTED
}
