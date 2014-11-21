#!/bin/bash
# The MIT License (MIT)
#
# Copyright (c) 2014 Harry Cummings
#
# Permission is hereby granted, free of charge, to any person obtaining a copy
# of this software and associated documentation files (the "Software"), to deal
# in the Software without restriction, including without limitation the rights
# to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
# copies of the Software, and to permit persons to whom the Software is
# furnished to do so, subject to the following conditions:
#
# The above copyright notice and this permission notice shall be included in all
# copies or substantial portions of the Software.
#
# THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
# IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
# FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
# AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
# LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
# OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
# SOFTWARE.
_s3_caching_dependencyFolder=$HOME/.m2
_s3_caching_file="cached.tar.bz2"

function getCachedDependencies {
    if [[ -z $(_s3_caching_diffPomFiles) ]]; then
        echo "pom.xml files unchanged - using cached dependencies"
        _s3_caching_downloadArchive
        _s3_caching_extractDependencies
    fi
}

function cacheDependencies {
    if [[ -n $(_s3_caching_diffPomFiles) ]]; then
        echo "pom.xml files have changed - updating cached dependencies"
        _s3_caching_compressDependencies
        _s3_caching_uploadArchive
    fi
}

function _s3_caching_diffPomFiles {
    git diff ${TRAVIS_COMMIT_RANGE} pom.xml **/pom.xml
}

_s3_caching_timeStamp=$(date -u +%Y%m%dT%H%M%SZ)
_s3_caching_dateStamp=$(date -u +%Y%m%d)
_s3_caching_region="us-east-1"
_s3_caching_service="s3"
_s3_caching_scope=${_s3_caching_dateStamp}/${_s3_caching_region}/${_s3_caching_service}/aws4_request
_s3_caching_host="${AWS_BUCKET}.s3.amazonaws.com"
_s3_caching_credential="${AWS_ACCESS_KEY_ID}/${_s3_caching_scope}"
_s3_caching_key=$(echo -n "AWS4${AWS_SECRET_ACCESS_KEY}" | xxd -c 256 -ps)
_s3_caching_signedHeaders="host;x-amz-content-sha256;x-amz-date"
_s3_caching_url="http://${_s3_caching_host}/${_s3_caching_file}"

function _s3_caching_downloadArchive {
    local contentHash="e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855"
    local authHeader=$(_s3_caching_generateAuthHeader "GET" ${contentHash})
    _s3_caching_responseCode=$(curl \
      -H "Host: ${_s3_caching_host}" \
      -H "X-amz-content-sha256: ${contentHash}" \
      -H "X-amz-date: ${_s3_caching_timeStamp}" \
      -H "Authorization: ${authHeader}" \
      -o ${_s3_caching_file} \
      -w "%{http_code}" \
      ${_s3_caching_url})
}

function _s3_caching_extractDependencies {
    if test ${_s3_caching_responseCode} -ne 200; then
        echo -n "${_s3_caching_responseCode} "
        cat ${_s3_caching_file}
    else
        tar --no-overwrite-dir -xjf ${_s3_caching_file} -C /
    fi
}

function _s3_caching_compressDependencies {
    tar -cjf ${_s3_caching_file} ${_s3_caching_dependencyFolder}
}

function _s3_caching_uploadArchive {
    local contentHash=$(cat ${_s3_caching_file} | openssl sha256 | _s3_caching_trimOpenSslOutput)
    local authHeader=$(_s3_caching_generateAuthHeader "PUT" ${contentHash})
    curl -X PUT -T "${_s3_caching_file}" \
      -H "Host: ${_s3_caching_host}" \
      -H "X-amz-content-sha256: ${contentHash}" \
      -H "X-amz-date: ${_s3_caching_timeStamp}" \
      -H "Authorization: ${authHeader}" \
      ${_s3_caching_url}
}

function _s3_caching_generateAuthHeader { # Args: HTTP method, content hash
    local canonicalRequest="$1\n/${_s3_caching_file}\n\n"
    canonicalRequest+="host:${_s3_caching_host}\nx-amz-content-sha256:$2\nx-amz-date:${_s3_caching_timeStamp}\n\n"
    canonicalRequest+="${_s3_caching_signedHeaders}\n$2"
    local hashedRequest=$(echo -en ${canonicalRequest} | openssl sha256 | _s3_caching_trimOpenSslOutput)
    local stringToSign="AWS4-HMAC-SHA256\n${_s3_caching_timeStamp}\n${_s3_caching_scope}\n${hashedRequest}"
    local signature=$(_s3_caching_sha256Hash $(_s3_caching_sha256Hash $(_s3_caching_sha256Hash $(_s3_caching_sha256Hash $(_s3_caching_sha256Hash \
      ${_s3_caching_key} ${_s3_caching_dateStamp}) ${_s3_caching_region}) ${_s3_caching_service}) "aws4_request") ${stringToSign})
    echo "AWS4-HMAC-SHA256 Credential=${_s3_caching_credential},SignedHeaders=${_s3_caching_signedHeaders},Signature=${signature}"
}

function _s3_caching_sha256Hash { #Args: hex key, data
    echo -en $2 | openssl dgst -sha256 -mac Hmac -macopt hexkey:$1 | _s3_caching_trimOpenSslOutput
}

function _s3_caching_trimOpenSslOutput {
    cut -d ' ' -f 2 $1
}