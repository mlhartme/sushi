/*
 * Copyright 1&1 Internet AG, http://www.1and1.org
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation; either version 2 of the License,
 * or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package de.ui.sushi.fs.webdav;

import java.io.IOException;

import de.ui.sushi.fs.Node;

public class WebdavNodeHttpFullTest extends WebdavNodeFullBase {
	static {
		WebdavFilesystem.wireLog(IO.guessProjectHome(WebdavNodeFullBase.class).getAbsolute() + "/target/http.log");
	}

    @Override
    protected Node createWork() throws IOException {
    	// see 
    	//  http://manas.tungare.name/blog/howto-setup-webdav-on-mac-os-x-leopard-for-syncing-omnifocus-to-iphone/
        // for setup macos instructions (omit auth settings)
	return IO.node("http://webdav.walter.ue.schlund.de/sushitests").deleteOpt().mkdir();
    }
}

