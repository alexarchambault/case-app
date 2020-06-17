/**
 * Copyright (c) 2017-present, Facebook, Inc.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

const React = require('react');

class Footer extends React.Component {
  docUrl(doc, language) {
    const baseUrl = this.props.config.baseUrl;
    const docsUrl = this.props.config.docsUrl;
    const docsPart = `${docsUrl ? `${docsUrl}/` : ''}`;
    const langPart = `${language ? `${language}/` : ''}`;
    return `${baseUrl}${docsPart}${langPart}${doc}`;
  }

  pageUrl(doc, language) {
    const baseUrl = this.props.config.baseUrl;
    return baseUrl + (language ? `${language}/` : '') + doc;
  }

  render() {
    return (
      <footer className="nav-footer" id="footer">
        <section className="sitemap">
          <h2>
            <a href={this.props.config.baseUrl} className="nav-home">
              CaseApp
            </a>
          </h2>

          <div>
            <h5>Overview</h5>
            <a href={this.docUrl('getting-started')}>Getting Started</a>
          </div>
          <div>
            <h5>Community</h5>
            <a href="https://github.com/alexarchambault/case-app" target="_blank">
              <img src="https://img.shields.io/github/stars/alexarchambault/case-app.svg?color=%23087e8b&label=stars&logo=github&style=social" />
            </a>
          </div>
        </section>

        <section className="copyright">{this.props.config.copyright}</section>
      </footer>
    );
  }
}

module.exports = Footer;
