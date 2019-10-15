/**
 * ============LICENSE_START====================================================
 * org.onap.aaf
 * ===========================================================================
 * Copyright (c) 2018 AT&T Intellectual Property. All rights reserved.
 * ===========================================================================
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 *      http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * ============LICENSE_END====================================================
 *
 */

package org.onap.aaf.auth.gui.pages;

import java.io.IOException;
import java.net.ConnectException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Comparator;
import java.util.List;

import org.onap.aaf.auth.env.AuthzEnv;
import org.onap.aaf.auth.env.AuthzTrans;
import org.onap.aaf.auth.gui.AAF_GUI;
import org.onap.aaf.auth.gui.BreadCrumbs;
import org.onap.aaf.auth.gui.NamedCode;
import org.onap.aaf.auth.gui.Page;
import org.onap.aaf.auth.gui.Table;
import org.onap.aaf.auth.gui.Table.Cells;
import org.onap.aaf.auth.gui.table.AbsCell;
import org.onap.aaf.auth.gui.table.TableData;
import org.onap.aaf.auth.gui.table.TextCell;
import org.onap.aaf.cadi.CadiException;
import org.onap.aaf.cadi.client.Future;
import org.onap.aaf.cadi.client.Rcli;
import org.onap.aaf.cadi.client.Retryable;
import org.onap.aaf.misc.env.APIException;
import org.onap.aaf.misc.env.Env;
import org.onap.aaf.misc.env.Slot;
import org.onap.aaf.misc.env.TimeTaken;
import org.onap.aaf.misc.xgen.Cache;
import org.onap.aaf.misc.xgen.DynamicCode;
import org.onap.aaf.misc.xgen.html.HTMLGen;

import aaf.v2_0.History;
import aaf.v2_0.History.Item;

public class NsHistory extends Page {
    static final String NAME="NsHistory";
    static final String HREF = "/gui/nsHistory";
    static final String FIELDS[] = {"name","dates"};
    static enum Month { JANUARY, FEBRUARY, MARCH, APRIL, MAY, JUNE, JULY, 
                            AUGUST, SEPTEMBER, OCTOBER, NOVEMBER, DECEMBER };

    public NsHistory(final AAF_GUI gui, final Page ... breadcrumbs) throws APIException, IOException {
        super(gui.env,NAME,HREF, FIELDS,
            new BreadCrumbs(breadcrumbs),
            new Table<AAF_GUI,AuthzTrans>("History", gui.env.newTransNoAvg(),new Model(gui.env),"class=std"),
            new NamedCode(true, "content") {
                @Override
                public void code(final Cache<HTMLGen> cache, final HTMLGen hgen) throws APIException, IOException {
                    final Slot name = gui.env.slot(NAME+".name");
                    cache.dynamic(hgen, new DynamicCode<HTMLGen, AAF_GUI, AuthzTrans>() {
                        @Override
                        public void code(final AAF_GUI gui, final AuthzTrans trans,    final Cache<HTMLGen> cache, final HTMLGen hgen)    throws APIException, IOException {
                            String obName = trans.get(name, null);
                        
                            // Use Javascript to make the table title more descriptive
                            hgen.js()
                            .text("var caption = document.querySelector(\".title\");")
                            .text("caption.innerHTML='History for Namespace [ " + obName + " ]';")                    
                            .done();
                        
                            // Use Javascript to change Link Target to our last visited Detail page
                            String lastPage = NsDetail.HREF + "?name=" + obName;
                            hgen.js()
                                .text("alterLink('nsdetail', '"+lastPage + "');")                        
                                .done();
                        
                            hgen.br();
                            hgen.leaf("a","href=#advanced_search","onclick=divVisibility('advanced_search');","class=greenbutton").text("Advanced Search").end()
                                .divID("advanced_search", "style=display:none");
                            hgen.incr("table");
                            
                            addDateRow(hgen,"Start Date");
                            addDateRow(hgen,"End Date");
                            hgen.incr("tr").incr("td");
                            hgen.tagOnly("input", "type=button","value=Get History",
                                    "onclick=datesURL('"+HREF+"?name=" + obName+"');","class=greenbutton");
                            hgen.end().end();
                            hgen.end();
                            hgen.end();
                            
                        }
                    });
                }
            }

            );
    }

    private static void addDateRow(HTMLGen hgen, String s) {
        hgen
            .incr("tr")
            .incr("td")
            .incr("label", "for=month", "required").text(s+"*").end()
            .end()
            .incr("td")
            .incr("select", "name=month"+s.substring(0, s.indexOf(' ')), "id=month"+s.substring(0, s.indexOf(' ')), "required")
            .incr("option", "value=").text("Month").end();
        for (Month m : Month.values()) {
            if (Calendar.getInstance().get(Calendar.MONTH) == m.ordinal()) {
                hgen.incr("option", "selected", "value="+(m.ordinal()+1)).text(m.name()).end();
            } else {
                hgen.incr("option", "value="+(m.ordinal()+1)).text(m.name()).end();
            }
        }
        hgen.end()
            .end()
            .incr("td")
            .tagOnly("input","type=number","id=year"+s.substring(0, s.indexOf(' ')),"required",
                    "value="+Calendar.getInstance().get(Calendar.YEAR), "min=1900", 
                    "max="+Calendar.getInstance().get(Calendar.YEAR),
                    "placeholder=Year").end()
            .end();
    }
    



    /**
     * Implement the Table Content for History
     * <p>
     * @author Jeremiah
     *
     */
    private static class Model extends TableData<AAF_GUI,AuthzTrans> {
        private static final String[] headers = new String[] {"Date","User","Memo"};
        private Slot name;
        private Slot dates;
    
        public Model(AuthzEnv env) {
            name = env.slot(NAME+".name");
            dates = env.slot(NAME+".dates");
        }
    
        @Override
        public String[] headers() {
            return headers;
        }
    
        @Override
        public Cells get(final AuthzTrans trans, final AAF_GUI gui) {
            final String oName = trans.get(name,null);
            final String oDates = trans.get(dates,null);
        
            if (oName==null) {
                return Cells.EMPTY;
            }
        
            final ArrayList<AbsCell[]> rv = new ArrayList<>();
            String msg = null;
            final TimeTaken tt = trans.start("AAF Get History for Namespace ["+oName+"]",Env.REMOTE);
            try {
                gui.clientAsUser(trans.getUserPrincipal(), new Retryable<Void>() {
                    @Override
                    public Void code(Rcli<?> client) throws CadiException, ConnectException, APIException {
                        if (oDates != null) {
                            client.setQueryParams("yyyymm="+oDates);
                        }
                        Future<History> fh = client.read("/authz/hist/ns/"+oName,gui.getDF(History.class));
                        if (fh.get(AAF_GUI.TIMEOUT)) {
                            tt.done();
                            TimeTaken tt2 = trans.start("Load History Data", Env.SUB);
                            try {
                                List<Item> histItems = fh.value.getItem();
                            
                                java.util.Collections.sort(histItems, new Comparator<Item>() {
                                    @Override
                                    public int compare(Item o1, Item o2) {
                                        return o2.getTimestamp().compare(o1.getTimestamp());
                                    }
                                });
                            
                                for (Item i : histItems) {
                                    String user = i.getUser();
                                    AbsCell userCell = new TextCell(user);
                                
                                    String memo = i.getMemo().replace("<script>", "&lt;script&gt;").replace("</script>", "&lt;/script&gt;");
                                    rv.add(new AbsCell[] {
                                            new TextCell(i.getTimestamp().toGregorianCalendar().getTime().toString()),
                                            userCell,
                                            new TextCell(memo)
                                    });
                                }
                            } finally {
                                tt2.done();
                            }
                        } else {
                            if (fh.code()==403) {
                                rv.add(new AbsCell[] {new TextCell("You may not view History of Namespace [" + oName + "]", "colspan = 3", "class=center")});
                            } else {
                                rv.add(new AbsCell[] {new TextCell("*** Data Unavailable ***", "colspan = 3", "class=center")});
                            }
                        }
                        return null;
                    }
                });
            } catch (Exception e) {
                trans.error().log(e);
            } finally {
                tt.done();
            }
        return new Cells(rv,msg);
        }
    }

}
