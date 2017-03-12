package com.google.firebase.quickstart.servlet;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * @author Joe Kutner on 3/12/17.
 *         Twitter: @codefinger
 */
public class MainServlet extends HttpServlet {
  @Override
  protected void doGet(HttpServletRequest req, HttpServletResponse resp)
      throws ServletException, IOException {

    resp.getWriter().print("Hello from the Firebase demo!");
  }
}
